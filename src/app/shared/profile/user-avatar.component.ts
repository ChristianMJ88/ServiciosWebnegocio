import { Component, computed, input, signal } from '@angular/core';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.css'
})
export class UserAvatarComponent {
  readonly imageUrl = input<string | null>(null);
  readonly initials = input('');

  private readonly rejectedImageUrl = signal<string | null>(null);

  readonly visibleImageUrl = computed(() => {
    const url = this.imageUrl()?.trim() || null;
    return url && url !== this.rejectedImageUrl() ? url : null;
  });

  rejectImage(): void {
    this.rejectedImageUrl.set(this.imageUrl()?.trim() || null);
  }

  validateImage(event: Event): void {
    const image = event.currentTarget as HTMLImageElement;
    if (!image.naturalWidth || !image.naturalHeight) {
      this.rejectImage();
      return;
    }

    try {
      const canvas = document.createElement('canvas');
      canvas.width = Math.min(image.naturalWidth, 32);
      canvas.height = Math.min(image.naturalHeight, 32);
      const context = canvas.getContext('2d', { willReadFrequently: true });
      if (!context) {
        return;
      }
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
      let hasVisiblePixel = false;
      for (let alphaIndex = 3; alphaIndex < pixels.length; alphaIndex += 4) {
        if (pixels[alphaIndex] > 16) {
          hasVisiblePixel = true;
          break;
        }
      }
      if (!hasVisiblePixel) {
        this.rejectImage();
      }
    } catch {
      // Remote images may prevent pixel inspection; a successful load remains valid.
    }
  }
}
