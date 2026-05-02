package com.example.appointment.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentMailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private AppointmentMailService appointmentMailService;

    @Test
    void sendReminderEmail_invokesMailSenderWhenFromAndRecipientSet() {
        ReflectionTestUtils.setField(appointmentMailService, "fromAddress", "clinic@example.com");
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        appointmentMailService.sendReminderEmail("patient@example.com", "Subject line", "Hello body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getFrom()).isEqualTo("clinic@example.com");
        assertThat(msg.getTo()).containsExactly("patient@example.com");
        assertThat(msg.getSubject()).isEqualTo("Subject line");
        assertThat(msg.getText()).isEqualTo("Hello body");
    }
}
