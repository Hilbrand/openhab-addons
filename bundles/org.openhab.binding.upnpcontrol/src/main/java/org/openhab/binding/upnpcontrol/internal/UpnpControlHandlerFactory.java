/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.upnpcontrol.internal;

import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.NOTIFICATION_AUDIOSINK_EXTENSION;
import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.SUPPORTED_THING_TYPES_UIDS;
import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.THING_TYPE_RENDERER;
import static org.openhab.binding.upnpcontrol.internal.UpnpControlBindingConstants.THING_TYPE_SERVER;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.binding.upnpcontrol.internal.audiosink.UpnpAudioSink;
import org.openhab.binding.upnpcontrol.internal.audiosink.UpnpAudioSinkReg;
import org.openhab.binding.upnpcontrol.internal.audiosink.UpnpNotificationAudioSink;
import org.openhab.binding.upnpcontrol.internal.config.UpnpControlBindingConfiguration;
import org.openhab.binding.upnpcontrol.internal.discovery.UpnpControlDiscoveryParticipant;
import org.openhab.binding.upnpcontrol.internal.handler.UpnpHandler;
import org.openhab.binding.upnpcontrol.internal.handler.UpnpRendererHandler;
import org.openhab.binding.upnpcontrol.internal.handler.UpnpServerHandler;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpnpControlHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Mark Herwege - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.upnpcontrol")
@NonNullByDefault
public class UpnpControlHandlerFactory extends BaseThingHandlerFactory implements UpnpAudioSinkReg, RegistryListener {
    final UpnpControlBindingConfiguration configuration = new UpnpControlBindingConfiguration();

    private final Logger logger = LoggerFactory.getLogger(UpnpControlHandlerFactory.class);

    private final ConcurrentMap<String, ServiceRegistration<AudioSink>> audioSinkRegistrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpnpRendererHandler> upnpRenderers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpnpServerHandler> upnpServers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpnpHandler> handlers = new ConcurrentHashMap<>();
    private ConcurrentMap<String, RemoteDevice> devices = new ConcurrentHashMap<>();

    private final UpnpIOService upnpIOService;
    private final UpnpService upnpService;
    private final AudioHTTPServer audioHTTPServer;
    private final NetworkAddressService networkAddressService;
    private final UpnpDynamicStateDescriptionProvider upnpStateDescriptionProvider;
    private final UpnpDynamicCommandDescriptionProvider upnpCommandDescriptionProvider;
    private final UpnpControlDiscoveryParticipant upnpControlDiscoveryParticipant;

    private String callbackUrl = "";

    @Activate
    public UpnpControlHandlerFactory(final @Reference UpnpIOService upnpIOService,
            @Reference final UpnpService upnpService, final @Reference AudioHTTPServer audioHTTPServer,
            final @Reference NetworkAddressService networkAddressService,
            final @Reference UpnpDynamicStateDescriptionProvider dynamicStateDescriptionProvider,
            final @Reference UpnpDynamicCommandDescriptionProvider dynamicCommandDescriptionProvider,
            final @Reference UpnpControlDiscoveryParticipant upnpControlDiscoveryParticipant,
            final Map<String, Object> config) {
        this.upnpIOService = upnpIOService;
        this.upnpService = upnpService;
        this.audioHTTPServer = audioHTTPServer;
        this.networkAddressService = networkAddressService;
        this.upnpStateDescriptionProvider = dynamicStateDescriptionProvider;
        this.upnpCommandDescriptionProvider = dynamicCommandDescriptionProvider;
        this.upnpControlDiscoveryParticipant = upnpControlDiscoveryParticipant;

        upnpService.getRegistry().addListener(this);

        modified(config);
    }

    @Modified
    protected void modified(final Map<String, Object> config) {
        // We update instead of replace the configuration object, so that if the user updates the
        // configuration, the values are automatically available in all handlers. Because they all
        // share the same instance.
        configuration.update(new Configuration(config).as(UpnpControlBindingConfiguration.class));
        logger.debug("Updated binding configuration to {}", configuration);
        upnpControlDiscoveryParticipant.setRemovalGracePeriodSeconds(configuration.removalGracePeriod);
    }

    @Deactivate
    protected void deActivate() {
        upnpService.getRegistry().removeListener(this);
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_RENDERER)) {
            return addRenderer(thing);
        } else if (thingTypeUID.equals(THING_TYPE_SERVER)) {
            return addServer(thing);
        }
        return null;
    }

    @Override
    public void unregisterHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        final String key = thing.getUID().toString();

        if (thingTypeUID.equals(THING_TYPE_RENDERER)) {
            removeRenderer(key);
        } else if (thingTypeUID.equals(THING_TYPE_SERVER)) {
            removeServer(key);
        }
        super.unregisterHandler(thing);
    }

    private UpnpServerHandler addServer(final Thing thing) {
        final UpnpServerHandler handler = new UpnpServerHandler(thing, upnpIOService, upnpRenderers,
                upnpStateDescriptionProvider, upnpCommandDescriptionProvider, configuration);
        final String key = thing.getUID().toString();
        upnpServers.put(key, handler);
        logger.debug("Media server handler created for {} with UID {}", thing.getLabel(), thing.getUID());

        final String udn = handler.getUDN();
        if (udn != null) {
            handlers.put(udn, handler);
            remoteDeviceUpdated(null, devices.get(udn));
        }

        return handler;
    }

    private UpnpRendererHandler addRenderer(final Thing thing) {
        callbackUrl = createCallbackUrl();
        final UpnpRendererHandler handler = new UpnpRendererHandler(thing, upnpIOService, this,
                upnpStateDescriptionProvider, upnpCommandDescriptionProvider, configuration);
        final String key = thing.getUID().toString();
        upnpRenderers.put(key, handler);
        upnpServers.forEach((thingId, value) -> value.addRendererOption(key));
        logger.debug("Media renderer handler created for {} with UID {}", thing.getLabel(), thing.getUID());

        final String udn = handler.getUDN();
        if (udn != null) {
            handlers.put(udn, handler);
            remoteDeviceUpdated(null, devices.get(udn));
        }

        return handler;
    }

    private void removeServer(final String key) {
        final UpnpHandler handler = upnpServers.get(key);
        if (handler == null) {
            return;
        }
        logger.debug("Removing media server handler for {} with UID {}", handler.getThing().getLabel(),
                handler.getThing().getUID());
        handlers.remove(handler.getUDN());
        upnpServers.remove(key);
    }

    private void removeRenderer(final String key) {
        final UpnpHandler handler = upnpServers.get(key);
        if (handler == null) {
            return;
        }
        logger.debug("Removing media renderer handler for {} with UID {}", handler.getThing().getLabel(),
                handler.getThing().getUID());

        if (audioSinkRegistrations.containsKey(key)) {
            logger.debug("Removing audio sink registration for {}", handler.getThing().getLabel());
            final ServiceRegistration<AudioSink> reg = audioSinkRegistrations.get(key);
            if (reg != null) {
                reg.unregister();
            }
            audioSinkRegistrations.remove(key);
        }

        final String notificationKey = key + NOTIFICATION_AUDIOSINK_EXTENSION;
        if (audioSinkRegistrations.containsKey(notificationKey)) {
            logger.debug("Removing notification audio sink registration for {}", handler.getThing().getLabel());
            final ServiceRegistration<AudioSink> reg = audioSinkRegistrations.get(notificationKey);
            if (reg != null) {
                reg.unregister();
            }
            audioSinkRegistrations.remove(notificationKey);
        }

        upnpServers.forEach((thingId, value) -> value.removeRendererOption(key));
        handlers.remove(handler.getUDN());
        upnpRenderers.remove(key);
    }

    @Override
    public void registerAudioSink(final UpnpRendererHandler handler) {
        if (!(callbackUrl.isEmpty())) {
            final UpnpAudioSink audioSink = new UpnpAudioSink(handler, audioHTTPServer, callbackUrl);
            @SuppressWarnings("unchecked")
            final ServiceRegistration<AudioSink> reg = (ServiceRegistration<AudioSink>) bundleContext
                    .registerService(AudioSink.class.getName(), audioSink, new Hashtable<String, Object>());
            final Thing thing = handler.getThing();
            audioSinkRegistrations.put(thing.getUID().toString(), reg);
            logger.debug("Audio sink added for media renderer {}", thing.getLabel());

            final UpnpNotificationAudioSink notificationAudioSink = new UpnpNotificationAudioSink(handler,
                    audioHTTPServer, callbackUrl);
            @SuppressWarnings("unchecked")
            final ServiceRegistration<AudioSink> notificationReg = (ServiceRegistration<AudioSink>) bundleContext
                    .registerService(AudioSink.class.getName(), notificationAudioSink, new Hashtable<String, Object>());
            audioSinkRegistrations.put(thing.getUID().toString() + NOTIFICATION_AUDIOSINK_EXTENSION, notificationReg);
            logger.debug("Notification audio sink added for media renderer {}", thing.getLabel());
        }
    }

    private String createCallbackUrl() {
        if (!callbackUrl.isEmpty()) {
            return callbackUrl;
        }
        final NetworkAddressService nwaService = networkAddressService;
        final String ipAddress = nwaService.getPrimaryIpv4HostAddress();
        if (ipAddress == null) {
            logger.warn("No network interface could be found.");
            return "";
        }
        final int port = HttpServiceUtil.getHttpServicePort(bundleContext);
        if (port == -1) {
            logger.warn("Cannot find port of the http service.");
            return "";
        }
        return "http://" + ipAddress + ":" + port;
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable final Registry registry, @Nullable final RemoteDevice device) {
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable final Registry registry, @Nullable final RemoteDevice device,
            @Nullable final Exception ex) {
    }

    @Override
    public void remoteDeviceAdded(@Nullable final Registry registry, @Nullable final RemoteDevice device) {
        if (device == null) {
            return;
        }

        final String udn = device.getIdentity().getUdn().getIdentifierString();
        if ("MediaServer".equals(device.getType().getType()) || "MediaRenderer".equals(device.getType().getType())) {
            devices.put(udn, device);
        }

        if (handlers.containsKey(udn)) {
            remoteDeviceUpdated(registry, device);
        }
    }

    @Override
    public void remoteDeviceUpdated(@Nullable final Registry registry, @Nullable final RemoteDevice device) {
        if (device == null) {
            return;
        }

        final String udn = device.getIdentity().getUdn().getIdentifierString();
        final UpnpHandler handler = handlers.get(udn);
        if (handler != null) {
            handler.updateDeviceConfig(device);
        }
    }

    @Override
    public void remoteDeviceRemoved(@Nullable final Registry registry, @Nullable final RemoteDevice device) {
        if (device == null) {
            return;
        }
        devices.remove(device.getIdentity().getUdn().getIdentifierString());
    }

    @Override
    public void localDeviceAdded(@Nullable final Registry registry, @Nullable final LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(@Nullable final Registry registry, @Nullable final LocalDevice device) {
    }

    @Override
    public void beforeShutdown(@Nullable final Registry registry) {
        devices = new ConcurrentHashMap<>();
    }

    @Override
    public void afterShutdown() {
    }
}
