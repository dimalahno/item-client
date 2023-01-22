package com.learnreactivespring.controller;

import com.learnreactivespring.domain.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.learnreactivespring.constants.ItemConstants.ITEM_END_POINT_V1;

@RestController
@Slf4j
public class ItemClientController {

    @ExceptionHandler
    public ResponseEntity<String> handleRuntimeException(RuntimeException exception) {
        log.error("Exception caught in handleRuntimeException : {} ", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }

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

    @GetMapping("/client/retrieve/singleItem/{id}")
    public Mono<Item> getOneItemUsingRetrieve(@PathVariable String id) {
        return webClient.get()
                .uri(ITEM_END_POINT_V1+"/{id}", id)
                .retrieve()
                .bodyToMono(Item.class);
    }

    @GetMapping("/client/retrieve/error")
    public Flux<Item> errorRetrieve() {
        return webClient.get()
                .uri(ITEM_END_POINT_V1+"/runtimeException")
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    Mono<String> errorMono = clientResponse.bodyToMono(String.class);
                    return errorMono.flatMap(error -> {
                        log.error("The error Messega is : {}", error);
                        return Mono.error(new RuntimeException(error));
                    });
                })
                .bodyToFlux(Item.class);
    }

    @GetMapping("/client/exchange/error")
    public Flux<Item> errorExchange() {
        return webClient.get()
                .uri(ITEM_END_POINT_V1+"/runtimeException")
                .exchange()
                .flatMapMany(clientResponse -> {
                    if (clientResponse.statusCode().is5xxServerError()) {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    log.error("The error Messega is : {}", errorMessage);
                                    return Mono.error(new RuntimeException(errorMessage));
                                });

                    } else {
                        return clientResponse.bodyToFlux(Item.class);
                    }
                });
    }

    @PostMapping("/client/createItem")
    public Mono<Item> createItem(@RequestBody Item newItem) {
        return webClient.post().uri(ITEM_END_POINT_V1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(newItem), Item.class)
                .retrieve()
                .bodyToMono(Item.class);
    }

    @PutMapping("/client/updateItem/{id}")
    public Mono<Item> updateItem(@PathVariable String id, @RequestBody Item actualItem) {
        return webClient.put()
                .uri(ITEM_END_POINT_V1+"/{id}", id)
                .body(Mono.just(actualItem), Item.class)
                .retrieve()
                .bodyToMono(Item.class);
    }

    @DeleteMapping("/client/deleteItem/{id}")
    public Mono<Void> deleteItem(@PathVariable String id) {
        return webClient.delete()
                .uri(ITEM_END_POINT_V1+"/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
