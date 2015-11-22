using UnityEngine;
using System.Collections;

public class CastResult : MonoBehaviour {
      public bool hit;
      public RaycastHit hitInfo;
}

public class OldCaster : MonoBehaviour {
   public static CastResult Raycast(Ray ray){
      RaycastHit hit;
      bool h = Physics.Raycast(ray, out hit);
      CastResult res = new CastResult();
      res.hit = h;
      res.hitInfo = hit;
      return res;
   }  
} 