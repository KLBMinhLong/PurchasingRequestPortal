import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page.component').then((m) => m.LoginPageComponent)
  },
  {
    path: 'users',
    canActivate: [authGuard],
    data: {
      roles: ['ADMIN']
    },
    loadComponent: () =>
      import('./features/user-management/pages/user-management-page/user-management-page.component').then(
        (m) => m.UserManagementPageComponent
      )
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'users'
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./features/auth/pages/forbidden-page/forbidden-page.component').then((m) => m.ForbiddenPageComponent)
  },
  {
    path: '**',
    redirectTo: 'users'
  }
];
