import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { DoctorResponse } from '../../shared/models/doctor.model';
import { DoctorVerification } from '../../shared/models/doctor-verification.model';
import { MessageDTO } from '../../shared/models/message.model'; // 👈 à créer si pas encore fait

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private newDoctorSubject = new Subject<DoctorResponse>();
  public newDoctor$ = this.newDoctorSubject.asObservable();

  private newVerificationSubject = new Subject<DoctorVerification>();
  public newVerification$ = this.newVerificationSubject.asObservable();

  private contractApprovedSubject = new Subject<DoctorVerification>();
  public contractApproved$ = this.contractApprovedSubject.asObservable();

  private newMessageSubject = new Subject<MessageDTO>();  // 👈
  public newMessage$ = this.newMessageSubject.asObservable();  // 👈

  private doctorClient: any = null;
  private verificationClient: any = null;
  private contractClient: any = null;
  private chatClient: any = null;  // 👈
  private reconnectInterval = 5000;
  private reconnectAttemptsDoctor = 0;
  private reconnectAttemptsVerification = 0;
  private reconnectAttemptsContract = 0;
  private reconnectAttemptsChat = 0;  // 👈
  private maxReconnectAttempts = 5;

  async connect() {
    try {
      const { Client } = await import('@stomp/stompjs');
      const token = localStorage.getItem('authToken') || '';

      const createClient = (
        brokerURL: string,
        topic: string,
        onMessage: (body: any) => void
      ) => {
        const config: any = {
          brokerURL,
          reconnectDelay: this.reconnectInterval,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          onConnect: () => {
            console.log(`✅ Connected to ${brokerURL}`);
            client.subscribe(topic, (message: any) => {
              try {
                const parsed = JSON.parse(message.body);
                onMessage(parsed);
              } catch (error) {
                console.error(`❌ Error parsing message from ${topic}:`, error);
              }
            });
          },
          onDisconnect: () => console.log(`⚠️ Disconnected from ${brokerURL}`),
          onStompError: (frame: any) => console.error('❌ STOMP error:', frame),
          onWebSocketError: (error: any) => console.error('❌ WebSocket error:', error),
        };

        if (token) {
          config.connectHeaders = { Authorization: `Bearer ${token}` };
        }

        const client = new Client(config);
        return client;
      };

      this.doctorClient = createClient(
        'ws://localhost:8081/ws',
        '/topic/doctors',
        (doctor) => this.newDoctorSubject.next(doctor)
      );

      this.verificationClient = createClient(
        'ws://localhost:8083/ws-doctor-verification',
        '/topic/doctor-verifications',
        (verification) => this.newVerificationSubject.next(verification)
      );

      this.contractClient = createClient(
        'ws://localhost:8083/ws-approve-contract',
        '/topic/contract-approved',
        (verification) => this.contractApprovedSubject.next(verification)
      );

      this.chatClient = createClient(  // 👈
        'ws://localhost:8083/ws-chat-messages',
        '/topic/chat-messages',
        (message) => this.newMessageSubject.next(message)
      );

      this.doctorClient.activate();
      this.verificationClient.activate();
      this.contractClient.activate();
      this.chatClient.activate();  // 👈

      console.log('🔌 Attempting to connect to all WebSocket endpoints...');
    } catch (error) {
      console.error('❌ Failed to initialize WebSocket:', error);
      this.attemptReconnect();
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttemptsDoctor < this.maxReconnectAttempts) {
      this.reconnectAttemptsDoctor++;
      console.log(`🔄 Reconnecting doctor WS (attempt ${this.reconnectAttemptsDoctor})...`);
      setTimeout(() => this.doctorClient?.activate(), this.reconnectInterval);
    }
    if (this.reconnectAttemptsVerification < this.maxReconnectAttempts) {
      this.reconnectAttemptsVerification++;
      console.log(`🔄 Reconnecting verification WS (attempt ${this.reconnectAttemptsVerification})...`);
      setTimeout(() => this.verificationClient?.activate(), this.reconnectInterval);
    }
    if (this.reconnectAttemptsContract < this.maxReconnectAttempts) {
      this.reconnectAttemptsContract++;
      console.log(`🔄 Reconnecting contract WS (attempt ${this.reconnectAttemptsContract})...`);
      setTimeout(() => this.contractClient?.activate(), this.reconnectInterval);
    }
    if (this.reconnectAttemptsChat < this.maxReconnectAttempts) {  // 👈
      this.reconnectAttemptsChat++;
      console.log(`🔄 Reconnecting chat WS (attempt ${this.reconnectAttemptsChat})...`);
      setTimeout(() => this.chatClient?.activate(), this.reconnectInterval);
    }
  }

  disconnect() {
    if (this.doctorClient?.active) this.doctorClient.deactivate();
    if (this.verificationClient?.active) this.verificationClient.deactivate();
    if (this.contractClient?.active) this.contractClient.deactivate();
    if (this.chatClient?.active) this.chatClient.deactivate();  // 👈
    console.log('🔌 All WebSockets disconnected');
  }
}