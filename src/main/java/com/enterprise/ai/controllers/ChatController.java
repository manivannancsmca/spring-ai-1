package com.enterprise.ai.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enterprise.ai.dtos.ActorsFilms;
import com.enterprise.ai.dtos.LanguageAnalysis;
import com.enterprise.ai.dtos.ProjectIdea;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/prompt1.st")
    private Resource promptResource;

    private static final String LANGUAGE_ANALYSIS_PROMPT = """
            Analyze the programming language: {language}

            popularity score: between 1-10, 1 is low, 10 is high
            learningDifficulty: either of these 3 values: "Easy", "Medium", "Hard"
            {format}
            """;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(
                        "You are a helpful AI assistant. You are expected to answer the questions related to the given domain."
                                +
                                "Any question that is asked outside of the domain, decline it by responding to user that "
                                +
                                "'I specialize in the given domain and I can't answer any query outside it'.")
                .build();
    }

    // Your Controller handler
    // → ChatClient.prompt().user("...").call()
    // → Spring AI builds a Prompt object
    // → OpenAiChatModel sends HTTP POST to api.openai.com/v1/chat/completions
    // → Response is parsed into ChatResponse
    // → .content() extracts the text
    //
    @GetMapping("/ask")
    public String ask() {
        return chatClient
                .prompt()
                .user("Hey, what is Spring AI?")
                .call()
                .content();
    }

    // streaming response
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream() {
        return chatClient
                .prompt()
                .user("Hey, what is Spring AI?")
                .stream()
                .content();
    }

    // prompt templates
    // inject some variables, and change prompt dynamically
    @GetMapping("/ask2")
    public String ask2(@RequestParam String domain, @RequestParam String question) {
        String promptTemplate = """
                You are a world-class expert in {domain}.
                Answer the following question clearly and concisely.

                Question: {question}
                """;

        return chatClient
                .prompt()
                .user(u -> u.text(promptTemplate)
                        .param("domain", domain)
                        .param("question", question))
                .call()
                .content();
    }

    // PromptTemplate class
    @GetMapping("/ask3")
    public String ask3(@RequestParam String domain, @RequestParam String question) {
        PromptTemplate promptTemplate = new PromptTemplate(promptResource);

        Prompt prompt = promptTemplate.create(Map.of("domain", domain, "question", question));

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    @GetMapping("/str-ask")
    public ActorsFilms strAsk() {
        ActorsFilms entity = chatClient
                .prompt()
                .user("Generate the filmography for a random actor.")
                .call()
                .entity(ActorsFilms.class);

        log.info("entity : {}", entity);

        return entity;
    }

    @GetMapping("/analyze-language")
    public LanguageAnalysis analyzeLanguage(@RequestParam String language) {

        BeanOutputConverter<LanguageAnalysis> converter = new BeanOutputConverter<>(LanguageAnalysis.class);

        return chatClient
                .prompt()
                .user(u -> u.text(LANGUAGE_ANALYSIS_PROMPT)
                        .param("language", language)
                        .param("format", converter.getFormat()))
                .call()
                .entity(LanguageAnalysis.class);
    }

    @GetMapping("/suggest-projects")
    public List<ProjectIdea> suggestProjects(@RequestParam String techStack) {

        var converter = new BeanOutputConverter<>(
                new ParameterizedTypeReference<List<ProjectIdea>>() {
                });

        return chatClient.prompt()
                .user(u -> u.text("""
                        Suggest 3 beginner project ideas for a developer using {techStack}.

                        {format}
                        """)
                        .param("techStack", techStack)
                        .param("format", converter.getFormat()))
                .call()
                .entity(new ParameterizedTypeReference<>() {
                });
    }

    @GetMapping("/ask4")
    public String ask(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

}
