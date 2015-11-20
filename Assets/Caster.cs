using UnityEngine;
using System.Collections;

public class Caster {
   public struct Result{
      public bool hit;
      public RaycastHit hitInfo;
   }

   public static Result Raycast(Ray ray){
      RaycastHit hit;
      bool h = Physics.Raycast(ray, out hit);
      Result res;
      res.hit = h;
      res.hitInfo = hit;
      return res;
   }
}