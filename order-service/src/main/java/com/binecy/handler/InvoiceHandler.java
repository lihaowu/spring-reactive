package com.binecy.handler;

import com.binecy.bean.Invoice;
import com.binecy.bean.Warehouse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class InvoiceHandler {

    public Mono<ServerResponse> get(ServerRequest request) {
        Invoice invoice = new Invoice();
        invoice.setId(999L);
        invoice.setOrderId(Long.parseLong(request.pathVariable("orderId")));
        return ok().contentType(APPLICATION_JSON).body(Mono.just(invoice), Warehouse.class);
    }
}
