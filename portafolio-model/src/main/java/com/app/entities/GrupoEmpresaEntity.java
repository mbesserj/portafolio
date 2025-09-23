package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grupo_empresas")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true) 
public class GrupoEmpresaEntity extends BaseEntity implements Serializable {

    @Column(name = "nombre_grupo")
    private String nombreGrupo;

    @OneToMany(mappedBy = "grupoEmpresa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmpresaEntity> empresas = new LinkedList<>();

}