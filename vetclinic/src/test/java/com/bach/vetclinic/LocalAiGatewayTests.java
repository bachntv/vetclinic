package com.bach.vetclinic;
import com.bach.vetclinic.service.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocalAiGatewayTests {
    HttpServer server;
    LocalAiGateway gateway;
    String completion;
    List<AssistantService.Source> sources=List.of(new AssistantService.Source("visit:1:0","Khám","viêm da","/visits/1","2026-10-21"));
    @BeforeEach void start()throws Exception {
        completion="Đã ghi nhận viêm da [visit:1:0]";
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/embed",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] body="{\"embeddings\":[[1,0],[1,0]]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.createContext("/api/chat",exchange->{
            String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(request.contains("visit:1:0"));
            byte[] body=("{\"message\":{\"content\":\""+completion+"\"}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.start();gateway=new LocalAiGateway(true,"http://127.0.0.1:"+server.getAddress().getPort(),"test","test-embed");
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void embeddingAndGenerationUseActualHttpProtocol(){assertEquals(sources,gateway.rank("chẩn đoán",sources));assertTrue(gateway.generate("chẩn đoán",sources).contains("[visit:1:0]"));}
    @Test void fabricatedCitationIsRejected(){completion="Bằng chứng [visit:999:0]";assertThrows(ResponseStatusException.class,()->gateway.generate("chẩn đoán",sources));}
    @Test void uncitedGenerationIsRejected(){completion="Không có dẫn nguồn";assertThrows(ResponseStatusException.class,()->gateway.generate("chẩn đoán",sources));}
    @Test void disabledModelDoesNotMakeNetworkCall(){var disabled=new LocalAiGateway(false,"http://127.0.0.1:1","test","test");assertThrows(ResponseStatusException.class,()->disabled.rank("query",sources));}
}
