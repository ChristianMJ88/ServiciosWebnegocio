
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  CatalogoSugeridoAdmin,
  GrupoServicioAdmin,
  GuardarGrupoServicioPayload,
  ImportarCatalogoSugeridoPayload,
  GuardarServicioPayload,
  GuardarSubgrupoServicioPayload,
  ServicioAdmin,
  SubgrupoServicioAdmin,
  SucursalAdmin
} from '../../core/admin/admin.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';

type SubgrupoServicioVista = SubgrupoServicioAdmin & {
  servicios: ServicioAdmin[];
};

type GrupoServicioVista = GrupoServicioAdmin & {
  subgrupos: SubgrupoServicioVista[];
  serviciosSinSubgrupo: ServicioAdmin[];
};

@Component({
  selector: 'app-admin-services-section',
  standalone: true,
  imports: [
    FormsModule,
    DragDropModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MoneyDisplayPipe,
    MatSelectModule,
    MatSlideToggleModule,
    MatTooltipModule
],
  templateUrl: './admin-services-section.component.html',
  styleUrl: './admin-services-section.component.css'
})
export class AdminServicesSectionComponent {
  @Input({ required: true }) catalogosSugeridos!: CatalogoSugeridoAdmin[];
  @Input({ required: true }) formularioImportarCatalogo!: ImportarCatalogoSugeridoPayload;
  @Input({ required: true }) importandoCatalogoSugerido!: boolean;
  @Input({ required: true }) grupoServicioEditandoId!: number | null;
  @Input({ required: true }) formularioGrupoServicio!: GuardarGrupoServicioPayload;
  @Input({ required: true }) guardandoGrupoServicio!: boolean;
  @Input({ required: true }) gruposServicio!: GrupoServicioAdmin[];
  @Input({ required: true }) subgrupoServicioEditandoId!: number | null;
  @Input({ required: true }) formularioSubgrupoServicio!: GuardarSubgrupoServicioPayload;
  @Input({ required: true }) guardandoSubgrupoServicio!: boolean;
  @Input({ required: true }) subgruposServicio!: SubgrupoServicioAdmin[];
  @Input({ required: true }) servicioEditandoId!: number | null;
  @Input({ required: true }) formularioServicio!: GuardarServicioPayload;
  @Input({ required: true }) sucursales!: SucursalAdmin[];
  @Input({ required: true }) guardandoServicio!: boolean;
  @Input({ required: true }) servicios!: ServicioAdmin[];

  @Output() clearGroupEdit = new EventEmitter<void>();
  @Output() saveGroup = new EventEmitter<void>();
  @Output() editGroup = new EventEmitter<GrupoServicioAdmin>();
  @Output() reorderGroups = new EventEmitter<GrupoServicioAdmin[]>();
  @Output() toggleGroupStatus = new EventEmitter<GrupoServicioAdmin>();
  @Output() importSuggestedCatalog = new EventEmitter<void>();
  @Output() clearSubgroupEdit = new EventEmitter<void>();
  @Output() saveSubgroup = new EventEmitter<void>();
  @Output() editSubgroup = new EventEmitter<SubgrupoServicioAdmin>();
  @Output() reorderSubgroups = new EventEmitter<SubgrupoServicioAdmin[]>();
  @Output() toggleSubgroupStatus = new EventEmitter<SubgrupoServicioAdmin>();
  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() editService = new EventEmitter<ServicioAdmin>();
  @Output() reorderServices = new EventEmitter<ServicioAdmin[]>();
  @Output() toggleServiceStatus = new EventEmitter<ServicioAdmin>();

  readonly orderStep = 10;
  activeForm: 'sugerencias' | 'grupo' | 'subgrupo' | 'servicio' = 'servicio';
  collapsedGroupIds = new Set<number>();
  collapsedSubgroupIds = new Set<number>();

  get subgruposFiltradosFormulario(): SubgrupoServicioAdmin[] {
    const grupoId = this.formularioServicio.grupoId;
    if (!grupoId) {
      return [];
    }
    return this.subgruposServicio.filter(subgrupo => subgrupo.grupoId === grupoId);
  }

  onGrupoServicioChange(rawGroupId: number | null) {
    const grupoId = rawGroupId ? Number(rawGroupId) : null;
    this.formularioServicio.grupoId = grupoId;
    if (!grupoId || !this.subgruposServicio.some(subgrupo => subgrupo.id === this.formularioServicio.subgrupoId && subgrupo.grupoId === grupoId)) {
      this.formularioServicio.subgrupoId = null;
    }
  }

  onGrupoSubgrupoChange(rawGroupId: number | null) {
    this.formularioSubgrupoServicio.grupoId = rawGroupId ? Number(rawGroupId) : null;
  }

  onSucursalesServicioChange(rawBranchIds: number[] | null) {
    const sucursalIds = (rawBranchIds ?? []).map(Number);
    this.formularioServicio.sucursalIds = sucursalIds;
    if (!sucursalIds.length) {
      this.formularioServicio.sucursalId = 0;
      return;
    }
    if (!sucursalIds.includes(this.formularioServicio.sucursalId)) {
      this.formularioServicio.sucursalId = sucursalIds[0];
    }
  }

  onSucursalPrincipalServicioChange(rawBranchId: number | null) {
    this.formularioServicio.sucursalId = rawBranchId ? Number(rawBranchId) : 0;
    if (this.formularioServicio.sucursalId && !this.formularioServicio.sucursalIds.includes(this.formularioServicio.sucursalId)) {
      this.formularioServicio.sucursalIds = [...this.formularioServicio.sucursalIds, this.formularioServicio.sucursalId];
    }
  }

  get sucursalesDisponiblesPrincipal(): SucursalAdmin[] {
    const ids = new Set(this.formularioServicio.sucursalIds ?? []);
    return this.sucursales.filter(sucursal => ids.has(sucursal.id));
  }

  gruposVista(): GrupoServicioVista[] {
    const gruposOrdenados = [...this.gruposServicio].sort(this.ordenarPorOrdenPublico);
    const subgruposOrdenados = [...this.subgruposServicio].sort(this.ordenarPorOrdenPublico);
    const serviciosOrdenados = [...this.servicios].sort(this.ordenarPorOrdenPublico);

    return gruposOrdenados.map(grupo => ({
      ...grupo,
      subgrupos: subgruposOrdenados
        .filter(subgrupo => subgrupo.grupoId === grupo.id)
        .map(subgrupo => ({
          ...subgrupo,
          servicios: serviciosOrdenados.filter(servicio => servicio.subgrupoId === subgrupo.id)
        })),
      serviciosSinSubgrupo: serviciosOrdenados.filter(servicio => servicio.grupoId === grupo.id && !servicio.subgrupoId)
    }));
  }

  get totalSubgrupos(): number {
    return this.subgruposServicio.length;
  }

  get totalServiciosActivos(): number {
    return this.servicios.filter(servicio => servicio.activo).length;
  }

  get serviciosSinGrupo(): ServicioAdmin[] {
    return [...this.servicios]
      .filter(servicio => !servicio.grupoId)
      .sort(this.ordenarPorOrdenPublico);
  }

  get subgroupDropListIds(): string[] {
    return this.gruposServicio.map(grupo => this.subgroupDropListId(grupo.id));
  }

  get serviceDropListIds(): string[] {
    return this.gruposVista().flatMap(grupo => [
      this.serviceDropListId(grupo.id, null),
      ...grupo.subgrupos.map(subgrupo => this.serviceDropListId(grupo.id, subgrupo.id))
    ]);
  }

  trackById(_index: number, item: { id: number }) {
    return item.id;
  }

  setActiveForm(form: 'sugerencias' | 'grupo' | 'subgrupo' | 'servicio') {
    this.activeForm = form;
  }

  editGroupFromCatalog(group: GrupoServicioAdmin) {
    this.activeForm = 'grupo';
    this.editGroup.emit(group);
  }

  editSubgroupFromCatalog(subgroup: SubgrupoServicioAdmin) {
    this.activeForm = 'subgrupo';
    this.editSubgroup.emit(subgroup);
  }

  editServiceFromCatalog(service: ServicioAdmin) {
    this.activeForm = 'servicio';
    this.editService.emit(service);
  }

  isGroupCollapsed(groupId: number): boolean {
    return this.collapsedGroupIds.has(groupId);
  }

  toggleGroupCollapse(groupId: number) {
    this.toggleSetValue(this.collapsedGroupIds, groupId);
  }

  isSubgroupCollapsed(subgroupId: number): boolean {
    return this.collapsedSubgroupIds.has(subgroupId);
  }

  toggleSubgroupCollapse(subgroupId: number) {
    this.toggleSetValue(this.collapsedSubgroupIds, subgroupId);
  }

  subgroupDropListId(groupId: number): string {
    return `subgrupos-${groupId}`;
  }

  serviceDropListId(groupId: number, subgroupId: number | null): string {
    return `servicios-${groupId}-${subgroupId ?? 'sin-subgrupo'}`;
  }

  onGroupsDrop(event: CdkDragDrop<GrupoServicioVista[]>) {
    const grupos = [...event.container.data];
    moveItemInArray(grupos, event.previousIndex, event.currentIndex);
    this.reorderGroups.emit(this.aplicarOrden(grupos));
  }

  onSubgroupsDrop(event: CdkDragDrop<SubgrupoServicioVista[]>, targetGroupId: number) {
    const previousGroupId = this.parseSubgroupDropListId(event.previousContainer.id);
    const previousList = [...event.previousContainer.data];
    const targetList = event.previousContainer === event.container
      ? previousList
      : [...event.container.data];

    if (event.previousContainer === event.container) {
      moveItemInArray(targetList, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(previousList, targetList, event.previousIndex, event.currentIndex);
    }

    const updates = [
      ...this.aplicarOrden(
        previousList.map(subgrupo => ({
          ...subgrupo,
          grupoId: previousGroupId ?? subgrupo.grupoId
        }))
      ),
      ...this.aplicarOrden(
        targetList.map(subgrupo => ({
          ...subgrupo,
          grupoId: targetGroupId
        }))
      )
    ];

    this.reorderSubgroups.emit(this.uniqueById(updates));
  }

  onServicesDrop(event: CdkDragDrop<ServicioAdmin[]>, targetGroupId: number, targetSubgroupId: number | null) {
    const previousTarget = this.parseServiceDropListId(event.previousContainer.id);
    const previousList = [...event.previousContainer.data];
    const targetList = event.previousContainer === event.container
      ? previousList
      : [...event.container.data];

    if (event.previousContainer === event.container) {
      moveItemInArray(targetList, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(previousList, targetList, event.previousIndex, event.currentIndex);
    }

    const updates = [
      ...this.aplicarOrden(
        previousList.map(servicio => ({
          ...servicio,
          grupoId: previousTarget?.groupId ?? servicio.grupoId,
          subgrupoId: previousTarget?.subgroupId ?? servicio.subgrupoId
        }))
      ),
      ...this.aplicarOrden(
        targetList.map(servicio => ({
          ...servicio,
          grupoId: targetGroupId,
          subgrupoId: targetSubgroupId
        }))
      )
    ];

    this.reorderServices.emit(this.uniqueById(updates));
  }

  get catalogoSugeridoSeleccionado(): CatalogoSugeridoAdmin | null {
    return this.catalogosSugeridos.find(catalogo => catalogo.id === this.formularioImportarCatalogo.sugerenciaId) ?? null;
  }

  onSuggestedCatalogChange(value: string) {
    this.formularioImportarCatalogo.sugerenciaId = value;
  }

  onSuggestedCatalogBranchChange(rawBranchId: number | null) {
    this.formularioImportarCatalogo.sucursalId = rawBranchId ? Number(rawBranchId) : null;
  }

  descripcionSucursales(servicio: ServicioAdmin): string {
    return (servicio.sucursalNombres ?? []).length
      ? servicio.sucursalNombres.join(', ')
      : servicio.sucursalNombre;
  }

  private ordenarPorOrdenPublico<T extends { ordenPublico: number; nombre: string }>(a: T, b: T): number {
    return a.ordenPublico - b.ordenPublico || a.nombre.localeCompare(b.nombre, 'es-MX');
  }

  private aplicarOrden<T extends { ordenPublico: number }>(items: T[]): T[] {
    return items.map((item, index) => ({
      ...item,
      ordenPublico: (index + 1) * this.orderStep
    }));
  }

  private uniqueById<T extends { id: number }>(items: T[]): T[] {
    const byId = new Map<number, T>();
    for (const item of items) {
      byId.set(item.id, item);
    }
    return Array.from(byId.values());
  }

  private parseSubgroupDropListId(id: string): number | null {
    const groupId = Number(id.replace('subgrupos-', ''));
    return Number.isFinite(groupId) ? groupId : null;
  }

  private parseServiceDropListId(id: string): { groupId: number; subgroupId: number | null } | null {
    const match = id.match(/^servicios-(\d+)-(.+)$/);
    if (!match) {
      return null;
    }
    return {
      groupId: Number(match[1]),
      subgroupId: match[2] === 'sin-subgrupo' ? null : Number(match[2])
    };
  }

  private toggleSetValue(set: Set<number>, value: number) {
    if (set.has(value)) {
      set.delete(value);
      return;
    }
    set.add(value);
  }
}
