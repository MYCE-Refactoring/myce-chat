package com.myce.domain.document;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "auto_sequence")
public class AutoIncrementSequence {
    @Id
    private String id;
    private Long seq;
}
