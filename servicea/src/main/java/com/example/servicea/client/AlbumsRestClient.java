package com.example.servicea.client;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange(
        url = "/albums",
        accept = MediaType.APPLICATION_JSON_VALUE)
public interface AlbumsRestClient {
    @GetExchange
    List<Album> getAlbums();

    record Album(String id, String userId, String title) {
    }
}
