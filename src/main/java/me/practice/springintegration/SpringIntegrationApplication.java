package me.practice.springintegration;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import me.practice.springintegration.cafe.Delivery;
import me.practice.springintegration.cafe.Drink;
import me.practice.springintegration.cafe.DrinkType;
import me.practice.springintegration.cafe.Order;
import me.practice.springintegration.cafe.OrderItem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(SpringIntegrationApplication.class, args);

        Cafe cafe = ctx.getBean(Cafe.class);
        for (int i = 1; i <= 1; i++) {
            Order order = new Order(i);
            order.addItem(DrinkType.LATTE, 2, false);
            order.addItem(DrinkType.MOCHA, 3, true);
            cafe.placeOrder(order);
        }

        System.out.println("Hit 'Enter' to terminate");
        System.in.read();
        ctx.close();
    }

    @MessagingGateway
    public interface Cafe {

        @Gateway(requestChannel = "orders.input")
        void placeOrder(Order order);

    }

    private final AtomicInteger hotDrinkCounter = new AtomicInteger();

    private final AtomicInteger coldDrinkCounter = new AtomicInteger();

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerSpec poller() {
        return Pollers.fixedDelay(1000);
    }

    @Bean
    public IntegrationFlow orders() {
        return f -> f
                .split(Order.class, Order::getItems)
                .channel(c -> c.executor(Executors.newCachedThreadPool()))
                .<OrderItem, Boolean>route(OrderItem::isIced, mapping -> mapping
                        .subFlowMapping(true, sf -> sf
                                .channel(c -> c.queue(10))
                                .publishSubscribeChannel(c -> c
                                        .subscribe(s -> s.handle(m -> sleepUninterruptibly(1, TimeUnit.SECONDS)))
                                        .subscribe(sub -> sub.<OrderItem, String>transform(
                                                p -> Thread.currentThread().getName() +
                                                                " prepared cold drink #" +
                                                                this.coldDrinkCounter.incrementAndGet() +
                                                                " for order #" + p.getOrderNumber() + ": " + p)
                                                .handle(m -> System.out.println(m.getPayload()))))
                                .bridge())
                        .subFlowMapping(false, sf -> sf
                                .channel(c -> c.queue(10))
                                .publishSubscribeChannel(c -> c
                                        .subscribe(s -> s.handle(m -> sleepUninterruptibly(5, TimeUnit.SECONDS)))
                                        .subscribe(sub -> sub
                                                .<OrderItem, String>transform(p ->
                                                        Thread.currentThread().getName() +
                                                                " prepared hot drink #" +
                                                                this.hotDrinkCounter.incrementAndGet() +
                                                                " for order #" + p.getOrderNumber() + ": " + p)
                                                .handle(m -> System.out.println(m.getPayload()))))
                                .bridge()))
                .<OrderItem, Drink>transform(orderItem ->
                        new Drink(orderItem.getOrderNumber(),
                                orderItem.getDrinkType(),
                                orderItem.isIced(),
                                orderItem.getShots()))
                .aggregate(aggregator -> aggregator
                        .outputProcessor(g ->
                                new Delivery(g.getMessages()
                                        .stream()
                                        .map(message -> (Drink) message.getPayload())
                                        .toList()))
                        .correlationStrategy(m -> ((Drink) m.getPayload()).getOrderNumber()))
                .handle(CharacterStreamWritingMessageHandler.stdout());
    }

    private static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            unit.sleep(sleepFor);
        }
        catch (InterruptedException e) {
            interrupted = true;
        }
        finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
