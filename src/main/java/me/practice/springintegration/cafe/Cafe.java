package me.practice.springintegration.cafe;

import org.springframework.core.annotation.Order;
import org.springframework.integration.annotation.Gateway;

public interface Cafe {

    @Gateway(requestChannel = "orders")
    void placeOrder(Order order);

}
