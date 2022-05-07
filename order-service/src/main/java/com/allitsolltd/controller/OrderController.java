package com.allitsolltd.controller;

import com.allitsolltd.client.InventoryClient;
import com.allitsolltd.dto.OrderDto;
import com.allitsolltd.model.Order;
import com.allitsolltd.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.cloud.stream.function.StreamBridge;


@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

	private final OrderRepository orderRepository;
	private final InventoryClient inventoryClient;
	private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final ExecutorService traceableExecutorService;
    
   private final StreamBridge streamBridge;



	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<Order> findAll() {
		return orderRepository.findAll();
	}

	@PostMapping("order")
	@ResponseStatus(HttpStatus.CREATED)
	public String placeOrder(@RequestBody OrderDto orderDto) {

		circuitBreakerFactory.configureExecutorService(traceableExecutorService);
		Resilience4JCircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory");
		java.util.function.Supplier<Boolean> booleanSupplier = () -> orderDto.getOrderLineItemsList().stream()
		.allMatch(lineItem -> {
			log.info("Making Call to Inventory Service for SkuCode {}", lineItem.getSkuCode());
			return inventoryClient.checkStock(lineItem.getSkuCode());
		});
		
		boolean productsInStock = circuitBreaker.run(booleanSupplier, throwable -> handleErrorCase());


		if (productsInStock) {
			Order order = new Order();
			order.setOrderLineItems(orderDto.getOrderLineItemsList());
			order.setOrderNumber(UUID.randomUUID().toString());

			orderRepository.save(order);
			
			  log.info("Sending Order Details with Order Id {} to Notification Service", order.getId());
	          streamBridge.send("notificationEventSupplier-out-0", MessageBuilder.withPayload(order.getId()).build());

			return "Order Place Successful";

		} else {

			return "Order Place Failed, one of the products in the order is not in stock";

		}

	}
	
	  private Boolean handleErrorCase() {
	        return false;
	    }
}
