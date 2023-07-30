package me.practice.springintegration.cafe;

import java.io.Serializable;
import java.util.List;

public class Delivery implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String SEPARATOR = "-----------------------";

    private List<Drink> deliveredDrinks;

    private int orderNumber;

    // Default constructor required by Jackson Java JSON-processor
    public Delivery() {}

    public Delivery(List<Drink> deliveredDrinks) {
        assert(deliveredDrinks.size() > 0);
        this.deliveredDrinks = deliveredDrinks;
        this.orderNumber = deliveredDrinks.get(0).getOrderNumber();
    }


    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<Drink> getDeliveredDrinks() {
        return deliveredDrinks;
    }

    public void setDeliveredDrinks(List<Drink> deliveredDrinks) {
        this.deliveredDrinks = deliveredDrinks;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(SEPARATOR + "\n");
        buffer.append("Order #" + getOrderNumber() + "\n");
        for (Drink drink : getDeliveredDrinks()) {
            buffer.append(drink);
            buffer.append("\n");
        }
        buffer.append(SEPARATOR + "\n");
        return buffer.toString();
    }
}
