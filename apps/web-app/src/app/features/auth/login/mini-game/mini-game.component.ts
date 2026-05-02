import { Component, OnInit, OnDestroy } from '@angular/core';

interface GardenPlant {
  x: number;
  y: number;
  emoji: string;
  id: number;
}

interface MemoryCard {
  id: number;
  emoji: string;
  flipped: boolean;
  matched: boolean;
}

type GameTab = 'breathe' | 'garden' | 'match';
type BreathPhase = 'in' | 'hold' | 'out' | 'rest';

@Component({
  selector: 'app-mini-game',
  templateUrl: './mini-game.component.html',
  styleUrls: ['./mini-game.component.scss']
})
export class MiniGameComponent implements OnInit, OnDestroy {
  activeTab: GameTab = 'breathe';

  // Breathing
  breathPhase: BreathPhase = 'in';
  breathLabel = 'Breathe in...';
  breathActive = false;
  private breathTimer: ReturnType<typeof setTimeout> | null = null;
  private phaseIndex = 0;
  private readonly phases: { phase: BreathPhase; label: string; duration: number }[] = [
    { phase: 'in', label: 'Breathe in...', duration: 4000 },
    { phase: 'hold', label: 'Hold...', duration: 4000 },
    { phase: 'out', label: 'Breathe out...', duration: 4000 },
    { phase: 'rest', label: 'Rest...', duration: 2000 }
  ];

  // Garden
  gardenPlants: GardenPlant[] = [];
  private plantId = 0;
  private readonly plantEmojis = ['🌸', '🌻', '🌷', '🌺', '🌼', '🌿', '🍀', '🌱', '🪻', '🌾'];

  // Memory
  memoryCards: MemoryCard[] = [];
  memoryMoves = 0;
  memoryWon = false;
  private flippedCards: MemoryCard[] = [];
  private lockBoard = false;
  private readonly cardEmojis = ['🪷', '🦋', '🌈', '☀️', '🍃', '🕊️'];

  ngOnInit(): void {
    this.startBreathing();
    this.initMemoryGame();
  }

  ngOnDestroy(): void {
    this.stopBreathing();
  }

  selectTab(tab: GameTab): void {
    this.activeTab = tab;
    if (tab === 'breathe' && !this.breathActive) {
      this.startBreathing();
    }
  }

  // --- Breathing ---

  startBreathing(): void {
    this.breathActive = true;
    this.phaseIndex = 0;
    this.applyPhase();
    this.cycleBreath();
  }

  private stopBreathing(): void {
    this.breathActive = false;
    if (this.breathTimer !== null) {
      clearTimeout(this.breathTimer);
      this.breathTimer = null;
    }
  }

  private cycleBreath(): void {
    const current = this.phases[this.phaseIndex];
    this.breathTimer = setTimeout(() => {
      this.phaseIndex = (this.phaseIndex + 1) % this.phases.length;
      this.applyPhase();
      if (this.breathActive) {
        this.cycleBreath();
      }
    }, current.duration);
  }

  private applyPhase(): void {
    const current = this.phases[this.phaseIndex];
    this.breathPhase = current.phase;
    this.breathLabel = current.label;
  }

  // --- Garden ---

  onGardenClick(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    const emoji = this.plantEmojis[Math.floor(Math.random() * this.plantEmojis.length)];
    this.gardenPlants.push({ x, y, emoji, id: this.plantId++ });
  }

  onGardenKeyPlant(): void {
    const x = Math.random() * 80 + 10;
    const y = Math.random() * 80 + 10;
    const emoji = this.plantEmojis[Math.floor(Math.random() * this.plantEmojis.length)];
    this.gardenPlants.push({ x, y, emoji, id: this.plantId++ });
  }

  clearGarden(): void {
    this.gardenPlants = [];
  }

  // --- Memory ---

  initMemoryGame(): void {
    const pairs = [...this.cardEmojis, ...this.cardEmojis];
    this.memoryCards = this.shuffle(pairs).map((emoji, i) => ({
      id: i,
      emoji,
      flipped: false,
      matched: false
    }));
    this.memoryMoves = 0;
    this.memoryWon = false;
    this.flippedCards = [];
    this.lockBoard = false;
  }

  flipCard(card: MemoryCard): void {
    if (this.lockBoard || card.flipped || card.matched) return;

    card.flipped = true;
    this.flippedCards.push(card);

    if (this.flippedCards.length === 2) {
      this.memoryMoves++;
      this.lockBoard = true;
      const [a, b] = this.flippedCards;

      if (a.emoji === b.emoji) {
        a.matched = true;
        b.matched = true;
        this.flippedCards = [];
        this.lockBoard = false;
        this.checkWin();
      } else {
        setTimeout(() => {
          a.flipped = false;
          b.flipped = false;
          this.flippedCards = [];
          this.lockBoard = false;
        }, 800);
      }
    }
  }

  private checkWin(): void {
    if (this.memoryCards.every(c => c.matched)) {
      this.memoryWon = true;
    }
  }

  private shuffle<T>(arr: T[]): T[] {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }
}
