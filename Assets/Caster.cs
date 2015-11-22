using UnityEngine;
using System.Collections;

public class Caster {
   public struct CastResult{
      public bool hit;
      public RaycastHit hitInfo;
   }

   public static CastResult Raycast(Ray ray){
      RaycastHit hit;
      bool h = Physics.Raycast(ray, out hit);
      CastResult res;
      res.hit = h;
      res.hitInfo = hit;
      return res;
   }
}