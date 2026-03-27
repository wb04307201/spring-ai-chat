package cn.wubo.spring.ai.chat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import static java.lang.Thread.sleep;

@Slf4j
@SpringBootTest(classes = ChatTestApplication.class)
class ChatTest {

    @Autowired
    private ChatModel chatModel;

    @Test
    void test() throws InterruptedException {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        var imageResource = new ClassPathResource("/test.jpg");
        log.info("imageResource: {}", imageResource.exists());

        Flux<String> response = chatClient
                .prompt()
                .user(u -> u.text("图片里有什么?")
                        .media(MimeTypeUtils.IMAGE_PNG, imageResource))
                .stream()
                .content();

        response.subscribe(log::info);

        Thread.sleep(1000*30);
    }
}
