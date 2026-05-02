package com.example.healthcare.service.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private SmtpEmailSender sender;

    @Test
    void send_whenFromMissing_throws() {
        ReflectionTestUtils.setField(sender, "fromAddress", "");

        assertThatThrownBy(() -> sender.send("to@x.com", "sub", "body"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mail configuration is incomplete");
        verifyNoInteractions(mailSender);
    }

    @Test
    void send_whenMailSenderThrows_wraps() {
        ReflectionTestUtils.setField(sender, "fromAddress", "from@x.com");
        doThrow(new RuntimeException("smtp fail")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send("to@x.com", "sub", "body"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send OTP email");
    }

    @Test
    void send_whenValid_sendsSimpleMailMessage() {
        ReflectionTestUtils.setField(sender, "fromAddress", "from@x.com");

        sender.send("to@x.com", "sub", "body");

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getFrom()).isEqualTo("from@x.com");
        assertThat(msgCaptor.getValue().getTo()).containsExactly("to@x.com");
        assertThat(msgCaptor.getValue().getSubject()).isEqualTo("sub");
        assertThat(msgCaptor.getValue().getText()).isEqualTo("body");
    }
}

