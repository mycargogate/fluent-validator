package ch.mycargogate.fluentValidator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ValidatorHolderTest {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Item implements HolderNode {
        private String identifier;
        private String description;

        @Override
        public String holderNodeName() {
            return identifier;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Order  implements HolderNode {
        private String identifier;
        private String hawb;
        private float value;

        private List<Item> items = new ArrayList<>();

        @Override
        public String holderNodeName() {
            return identifier;
        }
    }

    @Test
    public void testHolder() {
        var items = List.of(
                new Item("ITEM01", "White tee shirt"),
                new Item("ITEM02", "Blue sockets"),
                new Item("ITEM03", "Pink cap"),
                new Item("ITEM04", "Yellow pant")
        );

        var order = new Order("ORDER001", "123-456789", 606, items);

        var itemValidator = FluentValidator.<Item>builder()
                .fieldRule(Item::getIdentifier).mandatory().done()
                .fieldRule(Item::getDescription).regex("[a-zA-z]+").done()
                .build();

        var orderValidator = FluentValidator.<Order>builder()
                .fieldRule(Order::getIdentifier).mandatory().done()
                .fieldRule(Order::getHawb).mandatory().regex("[A-Za-z0-9]+").done()
                .fieldRule(Order::getValue).mandatory().min(0.1).done()
                .collectionRule(Order::getItems).elementValidator(itemValidator).done()
                .build();

        var result = orderValidator.validate(order);
        result.getErrors().forEach(System.out::println);
    }
}
