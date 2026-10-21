import { RenderMode, ServerRoute } from '@angular/ssr';

export const marketingServerRoutes: ServerRoute[] = [
  {path:'',renderMode:RenderMode.Prerender},
  {path:'**',renderMode:RenderMode.Client}
];
