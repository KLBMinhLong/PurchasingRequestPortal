import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './forbidden-page.component.html',
  styleUrl: './forbidden-page.component.scss'
})
export class ForbiddenPageComponent {}