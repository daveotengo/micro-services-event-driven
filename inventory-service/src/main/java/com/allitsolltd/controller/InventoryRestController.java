package com.allitsolltd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.allitsolltd.entity.Inventory;
import com.allitsolltd.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/inventory")
@RequiredArgsConstructor
public class InventoryRestController {
	private final InventoryRepository inventoryRepository;
	
	@GetMapping("/{skuCode}")
	public Boolean isInStock(@PathVariable String skuCode) {
		
		Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
				.orElseThrow(()->new RuntimeException("Cannot find Product by sku code "+skuCode));
		
		return inventory.getStock() > 0;
		
		
		
	}

}
