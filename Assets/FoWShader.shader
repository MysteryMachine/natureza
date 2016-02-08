Shader "Custom/FogOfWarCG" {
	Properties {
		_MainTex ("Base (RGB)", 2D) = "white" {}
	}
	SubShader {
		Tags { "RenderType" = "Transparent" "Queue" = "Transparent" }
		Blend SrcAlpha OneMinusSrcAlpha
		Cull Off
		LOD 200
		Lighting off
		
		pass {
			CGPROGRAM
			#pragma vertex vert_img
			#pragma fragment frag

			#include "UnityCG.cginc"
	        
			uniform sampler2D _MainTex;
			
			fixed4 frag(v2f_img i) : SV_Target {
				return tex2D(_MainTex, i.uv);
			}
			ENDCG
		}
	}
	FallBack "Diffuse"
}