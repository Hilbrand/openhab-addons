/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.renault.internal.handler;

import static org.openhab.binding.renault.internal.RenaultBindingConstants.ALL_CHANNELS;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_BATTERY_AVAILABLE_ENERGY;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_BATTERY_LEVEL;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_BATTERY_STATUS_UPDATED;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_CHARGING_REMAINING_TIME;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_CHARGING_STATUS;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_ESTIMATED_RANGE;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_EXTERNAL_TEMPERATURE;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_HVAC_STATUS;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_HVAC_TARGET_TEMPERATURE;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_IMAGE;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_LOCATION;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_LOCATION_UPDATED;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_LOCKED;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_ODOMETER;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_PAUSE;
import static org.openhab.binding.renault.internal.RenaultBindingConstants.CHANNEL_PLUG_STATUS;
import static org.openhab.core.library.unit.MetricPrefix.KILO;
import static org.openhab.core.library.unit.SIUnits.METRE;
import static org.openhab.core.library.unit.Units.KILOWATT_HOUR;
import static org.openhab.core.library.unit.Units.MINUTE;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.renault.internal.RenaultBindingConstants;
import org.openhab.binding.renault.internal.RenaultConfiguration;
import org.openhab.binding.renault.internal.api.Car;
import org.openhab.binding.renault.internal.api.Car.ChargingMode;
import org.openhab.binding.renault.internal.api.Car.LockStatus;
import org.openhab.binding.renault.internal.api.MyRenaultHttpSession;
import org.openhab.binding.renault.internal.api.exceptions.RenaultAPIGatewayException;
import org.openhab.binding.renault.internal.api.exceptions.RenaultActionException;
import org.openhab.binding.renault.internal.api.exceptions.RenaultException;
import org.openhab.binding.renault.internal.api.exceptions.RenaultForbiddenException;
import org.openhab.binding.renault.internal.api.exceptions.RenaultNotImplementedException;
import org.openhab.binding.renault.internal.api.exceptions.RenaultUpdateException;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RenaultHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Doug Culnane - Initial contribution
 */
@NonNullByDefault
public class RenaultHandler extends BaseThingHandler {

    private interface CommandHandler {
        void run() throws RenaultException, RenaultForbiddenException, RenaultUpdateException, RenaultActionException,
                RenaultNotImplementedException, InterruptedException, ExecutionException, TimeoutException;
    }

    private final Logger logger = LoggerFactory.getLogger(RenaultHandler.class);

    private final HttpClient httpClient;

    private RenaultConfiguration config = new RenaultConfiguration();

    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable MyRenaultHttpSession httpSession;

    private Car car;

    public RenaultHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.car = new Car();
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        // reset the car on initialize
        this.car = new Car();
        this.config = getConfigAs(RenaultConfiguration.class);

        // Validate configuration
        if (this.config.myRenaultUsername.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "MyRenault Username is empty!");
            return;
        }
        if (this.config.myRenaultPassword.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "MyRenault Password is empty!");
            return;
        }
        if (this.config.locale.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Location is empty!");
            return;
        }
        if (this.config.vin.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "VIN is empty!");
            return;
        }
        if (this.config.refreshInterval < 1) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The refresh interval mush to be larger than 1");
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        this.httpSession = new MyRenaultHttpSession(this.config, httpClient);

        reschedulePollingJob();
    }

    private void reschedulePollingJob() {
        final ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
        }
        pollingJob = scheduler.scheduleWithFixedDelay(this::getStatus, 0, config.refreshInterval, TimeUnit.MINUTES);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        final MyRenaultHttpSession httpSession = this.httpSession;
        if (httpSession == null) {
            return;
        }
        if (command instanceof RefreshType) {
            updateChannel(channelUID.getId());
            return;
        }
        switch (channelUID.getId()) {
            case RenaultBindingConstants.CHANNEL_HVAC_TARGET_TEMPERATURE:
                if (!car.isDisableHvac()) {
                    if (command instanceof DecimalType decimalCommand) {
                        car.setHvacTargetTemperature(decimalCommand.doubleValue());
                        updateState(CHANNEL_HVAC_TARGET_TEMPERATURE,
                                new QuantityType<Temperature>(car.getHvacTargetTemperature(), SIUnits.CELSIUS));
                    } else if (command instanceof QuantityType) {
                        @Nullable
                        QuantityType<Temperature> celsius = ((QuantityType<Temperature>) command)
                                .toUnit(SIUnits.CELSIUS);
                        if (celsius != null) {
                            car.setHvacTargetTemperature(celsius.doubleValue());
                        }
                        updateChannel(channelUID.getId());
                    }
                }
                break;
            case RenaultBindingConstants.CHANNEL_HVAC_STATUS:
                if (!car.isDisableHvac()) {
                    if (command instanceof StringType && command.toString().equals(Car.HVAC_STATUS_ON)) {
                        // We can only trigger pre-conditioning of the car.
                        perform(() -> {
                            updateState(CHANNEL_HVAC_STATUS, new StringType(Car.HVAC_STATUS_PENDING));
                            car.resetHVACStatus();
                            httpSession.initSesssion();
                            httpSession.actionHvacOn(car.getHvacTargetTemperature());
                            updateChannel(channelUID.getId());
                        }, "Error during action HVAC on.", true);
                    }
                }
                break;
            case RenaultBindingConstants.CHANNEL_CHARGING_MODE:
                if (command instanceof StringType) {
                    try {
                        ChargingMode newMode = ChargingMode.valueOf(command.toString());
                        if (!ChargingMode.UNKNOWN.equals(newMode)) {
                            perform(() -> {
                                httpSession.initSesssion();
                                httpSession.actionChargeMode(newMode);
                                car.setChargeMode(newMode);
                                updateChannel(channelUID.getId());
                            }, "Error during action set charge mode.", true);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid ChargingMode {}.", command.toString());
                        return;
                    }
                }
                break;
            case RenaultBindingConstants.CHANNEL_PAUSE:
                if (command instanceof OnOffType) {
                    try {
                        perform(() -> {
                            boolean pause = OnOffType.ON == command;
                            httpSession.initSesssion();
                            httpSession.actionPause(pause);
                            car.setPauseMode(pause);
                            updateState(CHANNEL_PAUSE, OnOffType.from(command.toString()));
                        }, "Error during action set pause.", true);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid Pause Mode {}.", command.toString());
                        return;
                    }
                }
                break;
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
        super.dispose();
    }

    private void getStatus() {
        final MyRenaultHttpSession httpSession = this.httpSession;
        if (httpSession == null) {
            return;
        }
        try {
            httpSession.initSesssion();
            performIfNotDisabled(car.getImageURL() != null, () -> httpSession.getVehicle(car), () -> {
            }, "imageURL");
            performIfNotDisabled(car.isDisableHvac(), () -> httpSession.getHvacStatus(car),
                    () -> car.setDisableHvac(true), "HVAC");
            performIfNotDisabled(car.isDisableLocation(), () -> httpSession.getLocation(car),
                    () -> car.setDisableLocation(true), "location");
            performIfNotDisabled(car.isDisableCockpit(), () -> httpSession.getCockpit(car),
                    () -> car.setDisableCockpit(true), "cockpit");
            performIfNotDisabled(car.isDisableBattery(), () -> httpSession.getBatteryStatus(car),
                    () -> car.setDisableBattery(true), "battery");
            performIfNotDisabled(car.isDisableLockStatus(), () -> httpSession.getLockStatus(car),
                    () -> car.setDisableLockStatus(true), "lock");

            ALL_CHANNELS.forEach(this::updateChannel);
            updateStatus(ThingStatus.ONLINE);
        } catch (InterruptedException e) {
            logger.warn("Error My Renault Http Session.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Error My Renault Http Session.", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateChannel(String channelId) {
        @Nullable
        State state = null;

        state = switch (channelId) {
            case CHANNEL_IMAGE -> stringNotBlank(car.getImageURL());
            // ── Location ─────────────────────────────────────────────────────────
            case CHANNEL_LOCATION ->
                getIfNotDisabled(car.isDisableLocation(), () -> point(car.getGpsLatitude(), car.getGpsLongitude()));
            case CHANNEL_LOCATION_UPDATED ->
                getIfNotDisabled(car.isDisableLocation(), () -> dateTime(car.getLocationUpdated()));
            // ── Cockpit ──────────────────────────────────────────────────────────
            case CHANNEL_ODOMETER ->
                getIfNotDisabled(car.isDisableCockpit(), () -> quantity(car.getOdometer(), KILO(METRE)));
            // ── Lock ──────────────────────────────────────────────────────────
            case CHANNEL_LOCKED -> getIfNotDisabled(car.isDisableLockStatus(), () -> lock(car.getLockStatus()));
            default -> null;
        };
        if (state == null && !car.isDisableHvac()) {
            // ── HVAC ─────────────────────────────────────────────────────────────
            state = switch (channelId) {
                case CHANNEL_HVAC_STATUS -> hvacStatus(car.getHvacstatus());
                case CHANNEL_HVAC_TARGET_TEMPERATURE -> quantity(car.getHvacTargetTemperature(), SIUnits.CELSIUS);
                case CHANNEL_EXTERNAL_TEMPERATURE -> quantity(car.getExternalTemperature(), SIUnits.CELSIUS);
                default -> null;
            };
        }
        // ── Battery ──────────────────────────────────────────────────────────
        if (state == null && !car.isDisableBattery()) {
            state = switch (channelId) {
                case CHANNEL_PLUG_STATUS -> new StringType(car.getPlugStatus().name());
                case CHANNEL_CHARGING_STATUS -> new StringType(car.getChargingStatus().name());
                case CHANNEL_BATTERY_LEVEL -> decimal(car.getBatteryLevel());
                case CHANNEL_ESTIMATED_RANGE -> quantity(car.getEstimatedRange(), KILO(METRE));
                case CHANNEL_BATTERY_AVAILABLE_ENERGY -> quantity(car.getBatteryAvailableEnergy(), KILOWATT_HOUR);
                case CHANNEL_CHARGING_REMAINING_TIME -> quantity(car.getChargingRemainingTime(), MINUTE);
                case CHANNEL_BATTERY_STATUS_UPDATED -> dateTime(car.getBatteryStatusUpdated());
                default -> null;
            };
        }
        if (state == null) {
            logger.debug("updateChannel: unhandled channel '{}'", channelId);
        }
        updateState(channelId, state == null ? UnDefType.UNDEF : state);
    }

    private State getIfNotDisabled(boolean disabled, Supplier<State> supplier) {
        return disabled ? UnDefType.UNDEF : supplier.get();
    }

    private static State stringNotBlank(@Nullable String value) {
        return value == null || value.isBlank() ? UnDefType.UNDEF : new StringType(value);
    }

    private State hvacStatus(@Nullable Boolean hvacStatus) {
        final String hvacString;
        if (hvacStatus == null) {
            hvacString = Car.HVAC_STATUS_PENDING;
        } else if (hvacStatus.booleanValue()) {
            hvacString = Car.HVAC_STATUS_ON;
        } else {
            hvacString = Car.HVAC_STATUS_OFF;
        }
        return new StringType(hvacString);
    }

    private static State lock(LockStatus lockStatus) {
        return switch (lockStatus) {
            case LOCKED -> OnOffType.ON;
            case UNLOCKED -> OnOffType.OFF;
            default -> UnDefType.UNDEF;
        };
    }

    private static State point(@Nullable Double latitude, @Nullable Double longitude) {
        return latitude == null || longitude == null ? UnDefType.UNDEF
                : new PointType(new DecimalType(latitude.doubleValue()), new DecimalType(longitude.doubleValue()));
    }

    private static State dateTime(@Nullable ZonedDateTime value) {
        return value == null ? UnDefType.UNDEF : new DateTimeType(value);
    }

    private static <Q extends Quantity<Q>> State quantity(@Nullable Number value, Unit<Q> unit) {
        return value == null ? UnDefType.UNDEF : new QuantityType<>(value, unit);
    }

    private static State decimal(@Nullable Number value) {
        return value == null ? UnDefType.UNDEF : new DecimalType(value);
    }

    private void performIfNotDisabled(boolean disabled, CommandHandler handler, Runnable disabler, String type) {
        if (disabled) {
            return;
        }
        perform(() -> {
            try {
                handler.run();
            } catch (RenaultNotImplementedException | RenaultAPIGatewayException e) {
                logger.debug("Disabling unsupported {} status update", type, e);
                disabler.run();
            }
        }, String.format("Error updating %s status.", type), false);
    }

    private void perform(CommandHandler handler, String errorMessage, boolean setOffline) {
        try {
            handler.run();
        } catch (InterruptedException e) {
            logger.warn("Error My Renault Http Session.", e);
            Thread.currentThread().interrupt();
        } catch (RenaultException | ExecutionException | TimeoutException e) {
            logger.warn("{}", errorMessage, e);
            if (setOffline) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }
}
