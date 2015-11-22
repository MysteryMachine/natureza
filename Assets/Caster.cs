using UnityEngine;
using System.Collections;

public class Caster : MonoBehaviour {
  public RaycastHit hit;
	
	// Update is called once per frame
	public bool Cast (Ray r) {
     return Physics.Raycast(r, out hit);
	} 
} 
