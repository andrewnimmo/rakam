package org.rakam.collection.mapper.geoip;


import com.google.common.base.Preconditions;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.timeZone;
import org.apache.avro.generic.GenericRecord;
import org.rakam.collection.Event;
import org.rakam.collection.FieldType;
import org.rakam.collection.SchemaField;
import org.rakam.collection.event.FieldDependencyBuilder;
import org.rakam.plugin.EventMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by buremba on 26/05/14.
 */
public class GeoIPEventMapper implements EventMapper {
    LookupService lookup;
    String[] attributes;

    public GeoIPEventMapper(GeoIPModuleConfig config) throws IOException {
        Preconditions.checkNotNull(config, "config is null");
        lookup = new LookupService(config.getDatabase(), LookupService.GEOIP_MEMORY_CACHE);
        attributes = config.getAttributes().stream().toArray(String[]::new);
    }

    @Override
    public void map(Event event) {
        GenericRecord properties = event.properties();
        String IP = (String) properties.get("ip");
        if (IP != null) {
            Location l1;
            try {
                l1 = lookup.getLocation(IP);
            } catch (Exception e) {
                return;
            }

            if(l1 == null) {
                return;
            }

            // TODO: we can compile a lambda that attaches appropriate attributes to events based on config values
            for (String attribute : attributes) {
                switch (attribute) {
                    case "country":
                        properties.put("country", l1.countryName);
                        break;
                    case "countryCode":
                        properties.put("countryCode", l1.countryCode);
                        break;
                    case "region":
                        properties.put("region", l1.region);
                        break;
                    case "city":
                        properties.put("city", l1.city);
                        break;
                    case "latitude":
                        properties.put("latitude", l1.latitude);
                        break;
                    case "longitude":
                        properties.put("longitude", l1.longitude);
                        break;
                    case "timezone":
                        String timezone = timeZone.timeZoneByCountryAndRegion(l1.countryCode, l1.region);
                        properties.put("timezone", timezone);
                        break;
                }
            }
        }
    }

    @Override
    public void addFieldDependency(FieldDependencyBuilder builder) {
        builder.addFields("ip", Arrays.stream(attributes)
                .map(attr -> new SchemaField(attr, FieldType.STRING, true))
                .collect(Collectors.toList()));
    }


}