import { Component, OnInit, OnDestroy } from '@angular/core';
import { MessagerieService } from '../../../core/services/messagerie.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { WebSocketService } from '../../../core/services/web-socket.service';

@Component({
  selector: 'app-messagerie',
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent implements OnInit, OnDestroy {

  messages: any[] = [];
  newMessage = '';

  menuVisible = false;
  menuX = 0;
  menuY = 0;
  selectedIndex = -1;

  editingIndex = -1;
  editText = '';

  conversations: any[] = [];
  filteredConversations: any[] = [];
  activeConversationId: number | null = null;
  activeConversationName: string = '';

  searchTerm: string = '';
  filteredUsers: any[] = [];
  private searchSubject = new Subject<string>();

  // Keyword search
  keywordSearchTerm: string = '';
  keywordSearchResults: any[] = [];
  keywordSearchActive: boolean = false;
  keywordSearchLoading: boolean = false;
  private keywordSearchSubject = new Subject<string>();

  searchActive: boolean = false;
  messageContent: string = '';
  currentUserId: number | null = null;

  conversationMenuVisible = false;
  conversationMenuX = 0;
  conversationMenuY = 0;
  selectedConversation: any = null;

  conversationAnalysis: any = null;
  analysisLoading = false;
  analysisMessage: string = '';
  analysisBadgeClass: string = '';

  private wsSubscription: Subscription | null = null;
  private clickListener!: () => void;

  constructor(
    private messagerieService: MessagerieService,
    public authService: AuthService,
    private userService: UserService,
    private webSocketService: WebSocketService
  ) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        if (!term.trim()) {
          this.filteredUsers = [];
          return [];
        }
        return this.messagerieService.searchUsers(term);
      })
    ).subscribe(users => this.filteredUsers = users);

    // Keyword search subscription
    this.keywordSearchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      switchMap(term => {
        if (!term.trim()) {
          this.keywordSearchResults = [];
          return [];
        }
        if (this.activeConversationId === null) {
          this.keywordSearchResults = [];
          return [];
        }
        this.keywordSearchLoading = true;
        return this.messagerieService.searchKeyword(term);
      })
    ).subscribe({
      next: (results) => {
        this.keywordSearchResults = (results || [])
          .map((result: any) => {
            const conversationId =
              result.conversationId ?? result.conversation_id ?? result.id ?? null;
            const participants =
              result.participants ??
              result.participantNames ??
              result.conversationName ??
              this.activeConversationName;
            const matchCount =
              result.matchCount ?? result.matches ?? result.count ?? 0;
            const lastMatchingMessage =
              result.lastMatchingMessage ??
              result.lastMessageContainingKeyword ??
              result.lastMatchedMessage ??
              result.lastMessage ??
              result.preview ??
              '';

            return {
              ...result,
              conversationId,
              participants,
              matchCount,
              lastMatchingMessage
            };
          })
          .filter((result: any) => result.conversationId === this.activeConversationId);
        this.keywordSearchLoading = false;
      },
      error: (err) => {
        console.error('Erreur recherche par mot-clé:', err);
        this.keywordSearchLoading = false;
      }
    });
  }

  ngOnInit() {
    // Store reference so we can remove it in ngOnDestroy
    this.clickListener = () => {
      this.menuVisible = false;
      this.conversationMenuVisible = false;
    };
    document.addEventListener('click', this.clickListener);

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) return;

    this.currentUserId = currentUser.userId;

    // Use switchMap to avoid nested subscriptions
    this.messagerieService.getUserConversations(this.currentUserId).pipe(
      switchMap(convos => {
        const otherUserIds = convos.map(c =>
          c.user1Id === this.currentUserId ? c.user2Id : c.user1Id
        );
        return this.userService.getUsersNamesById(otherUserIds).pipe(
          map(users => ({ convos, users }))
        );
      }),
      switchMap(({ convos, users }) => {
        // Fetch conversation summaries to get last messages
        return this.messagerieService.conversationSummary().pipe(
          map(summaries => ({ convos, users, summaries }))
        );
      })
    ).subscribe({
      next: ({ convos, users, summaries }) => {
        console.log('✅ Convos:', convos);
        console.log('👤 Users:', users);
        console.log('💬 Summaries:', summaries);

        // Try both 'id' and 'userId' field names to be safe
        const usersMap = new Map(
          users.map((u: any) => [u.id ?? u.userId, `${u.firstName} ${u.lastName}`])
        );

        // Create a map of conversation summaries by conversation ID
        const summaryMap = new Map(
          summaries.map((summary: any) => [summary.conversationId, summary.lastMessage])
        );

        this.conversations = convos.map((c: any) => ({
          ...c,
          otherUserName: usersMap.get(
            c.user1Id === this.currentUserId ? c.user2Id : c.user1Id
          ) ?? 'Unknown',
          lastMessage: summaryMap.get(c.id) || ''
        }));

        this.filteredConversations = [...this.conversations];
      },
      error: (err) => console.error('Erreur chargement conversations:', err)
    });

    // WebSocket
    this.webSocketService.connect();

    this.wsSubscription = this.webSocketService.newMessage$.subscribe((msg: any) => {
      if (msg.conversationId !== this.activeConversationId) return;

      // DELETE
      if (msg.deletedMessageId) {
        this.messages = this.messages.filter(m => m.id !== msg.deletedMessageId);
        return;
      }

      // UPDATE
      const existingIndex = this.messages.findIndex(m => m.id === msg.id);
      if (existingIndex !== -1) {
        this.messages[existingIndex].text = msg.content;
        return;
      }

      // ADD
      this.messages.push({
        id: msg.id,
        text: msg.content,
        type: msg.senderId === this.currentUserId ? 'sent' : 'received',
        createdAt: msg.createdAt,
        senderId: msg.senderId
      });
    });
  }

  ngOnDestroy() {
    // Remove the click listener to avoid memory leaks
    document.removeEventListener('click', this.clickListener);
    this.wsSubscription?.unsubscribe();
    this.webSocketService.disconnect();
  }

  onSearch() {
    this.searchSubject.next(this.searchTerm);
  }

  onFocusSearch() {
    this.searchActive = true;
  }

  cancelSearch() {
    this.searchActive = false;
    this.searchTerm = '';
    this.filteredUsers = [];
  }

  onKeywordSearch() {
    if (this.activeConversationId === null) {
      this.keywordSearchActive = false;
      this.keywordSearchResults = [];
      return;
    }
    this.keywordSearchSubject.next(this.keywordSearchTerm);
  }

  onKeywordSearchFocus() {
    if (this.activeConversationId === null) {
      return;
    }
    this.keywordSearchActive = true;
  }

  cancelKeywordSearch() {
    this.keywordSearchActive = false;
    this.keywordSearchTerm = '';
    this.keywordSearchResults = [];
  }

  selectSearchResult(result: any) {
    if (this.activeConversationId === null || result.conversationId !== this.activeConversationId) {
      return;
    }

    setTimeout(() => {
      const targetMessage = this.messages.find(m =>
        m.text.toLowerCase().includes(this.keywordSearchTerm.toLowerCase())
      );
      if (targetMessage) {
        const messageIndex = this.messages.indexOf(targetMessage);
        const messageElements = document.querySelectorAll('.message');
        if (messageElements[messageIndex]) {
          messageElements[messageIndex].scrollIntoView({ behavior: 'smooth', block: 'center' });
          (messageElements[messageIndex] as HTMLElement).classList.add('highlight-search');
          setTimeout(() => {
            (messageElements[messageIndex] as HTMLElement).classList.remove('highlight-search');
          }, 2000);
        }
      }
    }, 100);
  }

  selectUser(user: any) {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) return;

    this.messagerieService.startConversation(currentUser.userId, user.id).subscribe({
      next: (conversation) => {
        this.activeConversationId = conversation.id;
        this.activeConversationName = `${user.firstName} ${user.lastName}`;

        // Load analysis for this conversation
        this.loadConversationAnalysis(conversation.id);

        // Load messages for this conversation
        this.messagerieService.getConversationMessages(conversation.id).subscribe({
          next: (msgs) => {
            this.messages = msgs.map((msg: any) => ({
              text: msg.content || '',
              type: msg.senderId === this.currentUserId ? 'sent' : 'received',
              id: msg.id,
              createdAt: msg.createdAt,
              senderId: msg.senderId
            }));
          },
          error: (err) => {
            console.error('Erreur chargement messages:', err);
            this.messages = [];
          }
        });

        // ✅ Upsert conversation in list (fixes the "not appearing" bug)
        const existingIndex = this.conversations.findIndex(c => c.id === conversation.id);
        const convoEntry = {
          ...conversation,
          otherUserName: `${user.firstName} ${user.lastName}`,
          lastMessage: ''
        };

        if (existingIndex === -1) {
          this.conversations.unshift(convoEntry); // New: add at top
        } else {
          this.conversations[existingIndex] = convoEntry; // Existing: update in place
        }

        // ✅ Always sync filteredConversations BEFORE cancelSearch
        this.filteredConversations = [...this.conversations];
        this.cancelSearch();
        this.cancelKeywordSearch();

        // Load summary to populate lastMessage
        this.messagerieService.conversationSummary().subscribe({
          next: (summaries) => {
            const lastMsg = summaries.find((s: any) => s.conversationId === conversation.id)?.lastMessage || '';
            const convoToUpdate = this.conversations.find(c => c.id === conversation.id);
            if (convoToUpdate) {
              convoToUpdate.lastMessage = lastMsg;
              this.filteredConversations = [...this.conversations];
            }
          },
          error: (err) => console.error('Erreur chargement summary:', err)
        });
      },
      error: (err) => console.error('Erreur démarrage conversation:', err)
    });
  }

  selectConversation(convo: any) {
    this.activeConversationId = convo.id;
    this.activeConversationName = convo.otherUserName;
    this.cancelKeywordSearch();

    // Load analysis for this conversation
    this.loadConversationAnalysis(convo.id);

    this.messagerieService.getConversationMessages(convo.id).subscribe({
      next: (msgs) => {
        this.messages = msgs.map((msg: any) => ({
          text: msg.content || '',
          type: msg.senderId === this.currentUserId ? 'sent' : 'received',
          id: msg.id,
          createdAt: msg.createdAt,
          senderId: msg.senderId
        }));
      },
      error: (err) => {
        console.error('Erreur chargement messages:', err);
        this.messages = [];
      }
    });
  }

  sendMessage() {
    if (!this.messageContent.trim()) return;
    if (this.activeConversationId === null || this.currentUserId === null) return;

    const conversationId = this.activeConversationId;
    const senderId = this.currentUserId;
    const content = this.messageContent;

    this.messagerieService.sendMessages(conversationId, senderId, content).subscribe({
      next: (res) => {
        // Guard against WebSocket duplicate
        const alreadyExists = this.messages.some(m => m.id === res.id);
        if (!alreadyExists) {
          this.messages.push({
            id: res.id,
            text: res.content,
            type: 'sent',
            createdAt: res.createdAt,
            senderId: res.senderId
          });
        }
        this.messageContent = '';
      },
      error: (err) => console.error('Erreur envoi message:', err)
    });
  }

  openMenu(event: MouseEvent, index: number) {
    event.preventDefault();
    event.stopPropagation(); // Prevent immediate close from document click
    this.menuVisible = true;
    this.menuX = event.clientX;
    this.menuY = event.clientY;
    this.selectedIndex = index;
  }

  startEdit(index: number) {
    this.editingIndex = index;
    this.editText = this.messages[index].text;
    this.menuVisible = false;
  }

  saveEditMessage(index: number) {
    const msg = this.messages[index];
    if (!this.editText.trim() || msg.text === this.editText) {
      this.cancelEdit();
      return;
    }

    this.messagerieService.editMessage(msg.id, this.editText).subscribe({
      next: (updated) => {
        this.messages[index].text = updated.content;
        this.editingIndex = -1;
      },
      error: (err) => {
        console.error('Erreur modification message:', err);
        this.editingIndex = -1;
      }
    });
  }

  cancelEdit() {
    this.editingIndex = -1;
  }

  removeMessage() {
    if (this.selectedIndex < 0) return;
    const msg = this.messages[this.selectedIndex];

    this.messagerieService.deleteMessage(msg.id).subscribe({
      next: () => {
        this.messages.splice(this.selectedIndex, 1);
        this.menuVisible = false;
      },
      error: (err) => console.error('Erreur suppression message:', err)
    });
  }

  openConversationMenu(event: MouseEvent, convo: any) {
    event.preventDefault();
    event.stopPropagation(); // Prevent immediate close from document click
    this.conversationMenuVisible = true;
    this.conversationMenuX = event.clientX;
    this.conversationMenuY = event.clientY;
    this.selectedConversation = convo;
  }

  deleteConversationClicked() {
    if (!this.selectedConversation) return;

    const conversationId = this.selectedConversation.id;

    this.messagerieService.deleteConversation(conversationId).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conversationId);
        this.filteredConversations = [...this.conversations];

        if (this.activeConversationId === conversationId) {
          this.activeConversationId = null;
          this.activeConversationName = '';
          this.messages = [];
          this.conversationAnalysis = null;
        }

        this.conversationMenuVisible = false;
        this.selectedConversation = null;
      },
      error: (err) => console.error('Erreur suppression conversation:', err)
    });
  }

  loadConversationAnalysis(conversationId: number) {
    this.analysisLoading = true;
    this.conversationAnalysis = null;
    this.analysisMessage = '';
    this.analysisBadgeClass = '';
    this.messagerieService.analyseConversation(conversationId).subscribe({
      next: (analysis) => {
        this.conversationAnalysis = analysis;
        this.updateAnalysisMessage(analysis);
        this.analysisLoading = false;
      },
      error: (err) => {
        console.error('Erreur analyse conversation:', err);
        this.analysisLoading = false;
        this.conversationAnalysis = null;
      }
    });
  }

  updateAnalysisMessage(analysis: any) {
    const prediction = analysis?.prediction || 'Unknown';
    const confidence = analysis?.confidence || 0;
    const confidencePercent = (confidence * 100).toFixed(1);

    switch (prediction.toLowerCase()) {
      case 'suicidal':
        this.analysisBadgeClass = 'badge-suicidal';
        this.analysisMessage = `⚠️ SUICIDAL RISK DETECTED (${confidencePercent}% confidence) - This conversation shows indicators of suicidal ideation. Professional intervention may be necessary.`;
        break;
      case 'depression':
        this.analysisBadgeClass = 'badge-depression';
        this.analysisMessage = `😔 DEPRESSION INDICATORS (${confidencePercent}% confidence) - The conversation contains signs of depression. Consider providing mental health resources.`;
        break;
      case 'anxiety':
        this.analysisBadgeClass = 'badge-anxiety';
        this.analysisMessage = `😰 ANXIETY DETECTED (${confidencePercent}% confidence) - The conversation shows anxiety-related patterns. Support and reassurance may be helpful.`;
        break;
      case 'normal':
        this.analysisBadgeClass = 'badge-normal';
        this.analysisMessage = `✅ NORMAL (${confidencePercent}% confidence) - The conversation appears to be healthy and positive. No concerning patterns detected.`;
        break;
      default:
        this.analysisBadgeClass = 'badge-unknown';
        this.analysisMessage = `❓ ANALYSIS UNKNOWN - Unable to determine conversation status.`;
    }
  }
}