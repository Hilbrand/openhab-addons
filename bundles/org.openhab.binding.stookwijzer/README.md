# Stookwijzer Binding

Integrates the [RIVM Stookwijzer](https://www.rivm.nl/houtrook/stookwijzer) into openHAB.

Stookwijzer is a Dutch wood-burning advice service operated by RIVM (National Institute for Public Health and
the Environment). It combines wind force (Beaufort scale) and air quality index (LKI) to produce a colour-coded
advice per 4-digit postal code area: **yellow** (caution), **orange** (better not to burn), or **red** (do not burn).
Advice is issued in 6-hour time blocks.

Data source: RIVM WMS service — `https://data.rivm.nl/geo/alo/wms`

---

## Supported Things

| Thing Type ID          | Description                                               |
| ---------------------- | --------------------------------------------------------- |
| `stookwijzer:location` | Stookwijzer data for a single location in the Netherlands |

---

## Thing Configuration

The location is configured using **RD New coordinates** (EPSG:28992, in metres).

| Parameter         | Type    | Required | Default | Description                                                             |
| ----------------- | ------- | -------- | ------- | ----------------------------------------------------------------------- |
| `rdX`             | integer | yes      | 136372  | X-coordinate in RD New (EPSG:28992), metres. Valid range: 7000–300000   |
| `rdY`             | integer | yes      | 457489  | Y-coordinate in RD New (EPSG:28992), metres. Valid range: 289000–629000 |
| `refreshInterval` | integer | no       | 30      | Poll interval in minutes (min 5, max 360)                               |

> [!Note]
> **Finding RD New coordinates?** Use the PDOK Location Server https://api.pdok.nl/bzk/locationserver/search/v3_1/suggest?q=Utrecht
> Replace `Utrecht` with your location, or use your postal code.
> This will return a json file.
> Look for the `id` value and pass that value into the PDOK Location Server Lookup: https://api.pdok.nl/bzk/locationserver/search/v3_1/lookup?id=&lt;your id&gt;&fl=centroide_rd&wt=json.
> This will give you a Point with the x and y RD New coordinates.

---

## Channels

### Identification & timing

| Channel ID      | Item type | Description                                    |
| --------------- | --------- | ---------------------------------------------- |
| `model_runtime` | String    | Model run timestamp (format: dd-mm-yyyy hh:mm) |
| `pc4`           | String    | 4-digit postal code area                       |

### Meteorological data

| Channel ID     | Item type    | Unit | Description                                     |
| -------------- | ------------ | ---- | ----------------------------------------------- |
| `lki`          | Number       | –    | Air Quality Index 1–11 (-1 = no value)          |
| `wind_bft`     | Number       | Bft  | Average wind force in Beaufort for time block 1 |
| `wind`         | Number:Speed | m/s  | Average wind speed for time block 1             |
| `windrichting` | Number:Angle | °    | Wind direction in degrees (-1 = unavailable)    |

### Burning advice per time block

Each time block covers 6 hours after the model run. The model runs four times per day (04:00, 10:00, 16:00, 22:00).

| Channel ID  | Item type | Time block                  |
| ----------- | --------- | --------------------------- |
| `advies_0`  | Number    | 0–6 hours after model run   |
| `advies_12` | Number    | 12–18 hours after model run |
| `advies_18` | Number    | 18–24 hours after model run |
| `advies_6`  | Number    | 6–12 hours after model run  |

**Advice values:**

| Value | Code   | Meaning                              |
| ----- | ------ | ------------------------------------ |
| -1    | None   | No advice available                  |
| 0     | Yellow | Caution — burning may cause nuisance |
| 1     | Orange | Better not to burn wood right now    |
| 2     | Red    | Do not burn wood                     |

### Advice finalised per time block

| Channel ID      | Item type | Description                                        |
| --------------- | --------- | -------------------------------------------------- |
| `definitief_0`  | Switch    | ON when the advice for time block 0–6 h is final   |
| `definitief_12` | Switch    | ON when the advice for time block 12–18 h is final |
| `definitief_18` | Switch    | ON when the advice for time block 18–24 h is final |
| `definitief_6`  | Switch    | ON when the advice for time block 6–12 h is final  |

An advice becomes final at most **6 hours before** the start of its time block.

### LKI classification

| Value | Classification |
| ----- | -------------- |
| 11    | Very poor      |
| 1–2   | Good           |
| 3–4   | Moderate       |
| 5–7   | Insufficient   |
| 8–10  | Poor           |

---

### Example things file

```
Thing stookwijzer:location:home "Stookwijzer Home" [
    rdX=136372,
    rdY=457489,
    refreshInterval=30
]
```

## Example items file

```
String   Stookwijzer_PC4           "Postal code [%s]"            { channel="stookwijzer:location:home:pc4" }
String   Stookwijzer_ModelRun      "Model run [%s]"              { channel="stookwijzer:location:home:model_runtime" }

Number   Stookwijzer_LKI           "Air quality [%d]"            { channel="stookwijzer:location:home:lki" }
Number   Stookwijzer_Wind          "Wind [%.1f m/s]"             { channel="stookwijzer:location:home:wind" }
Number   Stookwijzer_WindBft       "Wind force [%d Bft]"         { channel="stookwijzer:location:home:wind_bft" }
Number   Stookwijzer_Windrichting  "Wind direction [%d°]"        { channel="stookwijzer:location:home:windrichting" }

Number   Stookwijzer_Advies0       "Advice now [%d]"             { channel="stookwijzer:location:home:advies_0" }
Number   Stookwijzer_Advies6       "Advice +6h [%d]"             { channel="stookwijzer:location:home:advies_6" }
Number   Stookwijzer_Advies12      "Advice +12h [%d]"            { channel="stookwijzer:location:home:advies_12" }
Number   Stookwijzer_Advies18      "Advice +18h [%d]"            { channel="stookwijzer:location:home:advies_18" }

Switch   Stookwijzer_Def0          "Final now [%s]"              { channel="stookwijzer:location:home:definitief_0" }
Switch   Stookwijzer_Def6          "Final +6h [%s]"              { channel="stookwijzer:location:home:definitief_6" }
Switch   Stookwijzer_Def12         "Final +12h [%s]"             { channel="stookwijzer:location:home:definitief_12" }
Switch   Stookwijzer_Def18         "Final +18h [%s]"             { channel="stookwijzer:location:home:definitief_18" }
```

---

## Example rule — notification on red advice

```
rule "Stookwijzer red alert"
when
    Item Stookwijzer_Advies0 changed to 2
then
    sendNotification("user@example.com",
        "🔴 Stookwijzer: RED — Do not burn wood!")
end
```

---

## Technical details

- Uses the RIVM WMS GetFeatureInfo API (OGC:WMS 1.3.0), layer `stookwijzer_v2`
- CRS: EPSG:28992 (RD New)
- BBOX is calculated as `x,y,x+1,y+1` (1×1 metre bounding box around the configured location point)
- Advice is published in 6-hour blocks; the service updates four times per day
- All channels are read-only
