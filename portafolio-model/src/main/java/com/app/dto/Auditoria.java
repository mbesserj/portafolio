package com.app.dto;

import java.time.LocalDate;
import java.util.Objects;

public class Auditoria {

    private Long id;
    private LocalDate fechaAuditoria;
    private String tipoEntidad;
    private String valorClave;
    private String descripcion;
    private String ArchivoOrigen;
    private LocalDate fechaArchivo;
    private LocalDate fechaDatos;
    private int filaNumero;
    private String motivo;
    private int registrosInsertados;
    private int registrosRechazados;
    private int registrosDuplicados;

    public Auditoria() {
    }

    public Auditoria(Long id, LocalDate fechaAuditoria, String tipoEntidad, String valorClave, String descripcion, String archivoOrigen, LocalDate fechaArchivo, LocalDate fechaDatos, int filaNumero, String motivo, int registrosInsertados, int registrosRechazados, int registrosDuplicados) {
        this.id = id;
        this.fechaAuditoria = fechaAuditoria;
        this.tipoEntidad = tipoEntidad;
        this.valorClave = valorClave;
        this.descripcion = descripcion;
        ArchivoOrigen = archivoOrigen;
        this.fechaArchivo = fechaArchivo;
        this.fechaDatos = fechaDatos;
        this.filaNumero = filaNumero;
        this.motivo = motivo;
        this.registrosInsertados = registrosInsertados;
        this.registrosRechazados = registrosRechazados;
        this.registrosDuplicados = registrosDuplicados;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFechaAuditoria() {
        return fechaAuditoria;
    }

    public void setFechaAuditoria(LocalDate fechaAuditoria) {
        this.fechaAuditoria = fechaAuditoria;
    }

    public String getTipoEntidad() {
        return tipoEntidad;
    }

    public void setTipoEntidad(String tipoEntidad) {
        this.tipoEntidad = tipoEntidad;
    }

    public String getValorClave() {
        return valorClave;
    }

    public void setValorClave(String valorClave) {
        this.valorClave = valorClave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getArchivoOrigen() {
        return ArchivoOrigen;
    }

    public void setArchivoOrigen(String archivoOrigen) {
        ArchivoOrigen = archivoOrigen;
    }

    public LocalDate getFechaArchivo() {
        return fechaArchivo;
    }

    public void setFechaArchivo(LocalDate fechaArchivo) {
        this.fechaArchivo = fechaArchivo;
    }

    public LocalDate getFechaDatos() {
        return fechaDatos;
    }

    public void setFechaDatos(LocalDate fechaDatos) {
        this.fechaDatos = fechaDatos;
    }

    public int getFilaNumero() {
        return filaNumero;
    }

    public void setFilaNumero(int filaNumero) {
        this.filaNumero = filaNumero;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public int getRegistrosInsertados() {
        return registrosInsertados;
    }

    public void setRegistrosInsertados(int registrosInsertados) {
        this.registrosInsertados = registrosInsertados;
    }

    public int getRegistrosRechazados() {
        return registrosRechazados;
    }

    public void setRegistrosRechazados(int registrosRechazados) {
        this.registrosRechazados = registrosRechazados;
    }

    public int getRegistrosDuplicados() {
        return registrosDuplicados;
    }

    public void setRegistrosDuplicados(int registrosDuplicados) {
        this.registrosDuplicados = registrosDuplicados;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Auditoria auditoria = (Auditoria) o;
        return getFilaNumero() == auditoria.getFilaNumero() && getRegistrosInsertados() == auditoria.getRegistrosInsertados() && getRegistrosRechazados() == auditoria.getRegistrosRechazados() && getRegistrosDuplicados() == auditoria.getRegistrosDuplicados() && Objects.equals(getId(), auditoria.getId()) && Objects.equals(getFechaAuditoria(), auditoria.getFechaAuditoria()) && Objects.equals(getTipoEntidad(), auditoria.getTipoEntidad()) && Objects.equals(getValorClave(), auditoria.getValorClave()) && Objects.equals(getDescripcion(), auditoria.getDescripcion()) && Objects.equals(getArchivoOrigen(), auditoria.getArchivoOrigen()) && Objects.equals(getFechaArchivo(), auditoria.getFechaArchivo()) && Objects.equals(getFechaDatos(), auditoria.getFechaDatos()) && Objects.equals(getMotivo(), auditoria.getMotivo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFechaAuditoria(), getTipoEntidad(), getValorClave(), getDescripcion(), getArchivoOrigen(), getFechaArchivo(), getFechaDatos(), getFilaNumero(), getMotivo(), getRegistrosInsertados(), getRegistrosRechazados(), getRegistrosDuplicados());
    }

    @Override
    public String toString() {
        return "Auditoria{" +
                "id=" + id +
                ", fechaAuditoria=" + fechaAuditoria +
                ", tipoEntidad='" + tipoEntidad + '\'' +
                ", valorClave='" + valorClave + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", ArchivoOrigen='" + ArchivoOrigen + '\'' +
                ", fechaArchivo=" + fechaArchivo +
                ", fechaDatos=" + fechaDatos +
                ", filaNumero=" + filaNumero +
                ", motivo='" + motivo + '\'' +
                ", registrosInsertados=" + registrosInsertados +
                ", registrosRechazados=" + registrosRechazados +
                ", registrosDuplicados=" + registrosDuplicados +
                '}';
    }
}