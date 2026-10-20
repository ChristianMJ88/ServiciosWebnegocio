export default {
  test: {
    // Angular CLI usa isolate:false por defecto. Cada archivo necesita su propio TestBed
    // y entorno DOM para que un timeout o estado global no contamine otro archivo.
    isolate: true
  }
};
