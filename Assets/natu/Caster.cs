using UnityEngine;
using System.Collections;

public class Caster {
   public RaycastHit hit;
   public bool success;
   public Caster(Ray ray){
      success = Physics.Raycast(ray, out hit);
   }
} 