
package com.example.validator;

import com.example.validator.csv.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ValidatorCSVTest {
    record Order(boolean vip, int age, double discount, LocalDate start, LocalDate end) {}

    @Test
    void csvBuildAndValidate() throws Exception {
        String csv = String.join("\n",
            "field,age,optional",
            "field,age,min,0",
            "field,age,max,120",
            "field,age,customValue,evenAge,\"age must be even\"",
            "field,discount,customWithObject,nonVipMax10,\"non-VIP discount must be ≤ 10%\"",
            "object,,objectCustom,datesOk,\"end must be on/after start\""
        );
        var reg = new DefaultRegistry<Order>()
                .addValue("evenAge", v -> ((Number)v).intValue() % 2 == 0)
                .addObjectAndValue("nonVipMax10", (o,v) -> o.vip() || ((Number)v).doubleValue() <= 0.1)
                .addObject("datesOk", o -> o.end()==null || !o.end().isBefore(o.start()));
        var in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        var v = new com.example.validator.csv.ValidatorReaderCSV<Order>().load(in, reg);

        var r1 = v.validate(new Order(false, 24, 0.2, LocalDate.now(), LocalDate.now().minusDays(1)));
        assertFalse(r1.isValid());
    }
}
