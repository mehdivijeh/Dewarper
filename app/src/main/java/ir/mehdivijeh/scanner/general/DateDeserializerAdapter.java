package ir.mehdivijeh.scanner.general;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * define DateDeserializerAdapter as convert JSON DateTime as Date Value with UTC format
 */
public class DateDeserializerAdapter implements JsonSerializer<Long>,
        JsonDeserializer<Long> {
    private final DateFormat dateFormat;

    public DateDeserializerAdapter() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public synchronized JsonElement serialize(Long date, Type type,
                                              JsonSerializationContext jsonSerializationContext) {
        synchronized (dateFormat) {
            String dateFormatAsString = dateFormat.format(new Date(date));
            return new JsonPrimitive(dateFormatAsString);
        }
    }

    @Override
    public synchronized Long deserialize(JsonElement jsonElement, Type type,
                                         JsonDeserializationContext jsonDeserializationContext) {
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(jsonElement.getAsString()).getTime();
            }
        } catch (ParseException e) {
            throw new JsonSyntaxException(jsonElement.getAsString(), e);
        }
    }
}