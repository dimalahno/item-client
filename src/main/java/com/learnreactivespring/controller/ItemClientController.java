package com.learnreactivespring.controller;

import com.learnreactivespring.domain.Item;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Objects;

import static com.learnreactivespring.constants.ItemConstants.ITEM_END_POINT_V1;

@RestController
public class ItemClientController {

    private final WebClient webClient;

    public ItemClientController(Environment env) {
        webClient = WebClient.create(Objects.requireNonNull(env.getProperty("webclient.url")));
    }

    @GetMapping("/client/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve() {
        return webClient.get()
                .uri(ITEM_END_POINT_V1)
                .retrieve()
                .bodyToFlux(Item.class);
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsUsingExchange() {
        return webClient.get()
                .uri(ITEM_END_POINT_V1)
                .exchange()
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class));
    }
}
