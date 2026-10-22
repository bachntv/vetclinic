package com.bach.vetclinic.entity;
import jakarta.persistence.*;
@Entity @Table(name="vaccine_types")
public class VaccineType extends BaseEntity {
    @Column(nullable=false,length=120) private String name;
    @Column(length=200) private String manufacturer;
    @Column(columnDefinition="text") private String description;
    @Column(nullable=false) private boolean active=true;
    protected VaccineType(){}
    public VaccineType(String name,String manufacturer,String description){update(name,manufacturer,description);}
    public void update(String name,String manufacturer,String description){this.name=name;this.manufacturer=manufacturer;this.description=description;}
    public String getName(){return name;} public String getManufacturer(){return manufacturer;}
    public String getDescription(){return description;} public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
}
