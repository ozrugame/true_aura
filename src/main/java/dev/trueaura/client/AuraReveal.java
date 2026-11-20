package dev.trueaura.client;

/** Growing tongues of fire from the feet; completed appearance preserves the original mesh exactly. */
public final class AuraReveal {
    public static double height(double progress,double u,double layer,double time) {
        double p=Math.max(0,Math.min(1,progress));
        double flutter=.24*Math.sin(u*Math.PI*14-time*6+layer*.7)+.1*Math.sin(u*Math.PI*22+time*4);
        return Math.max(0,Math.min(1,p+p*(1-p)*flutter));
    }
    public static AuraMesh.VertexSink wrap(AuraMesh.VertexSink sink,float progress,double time) {
        if(progress>=1) return sink;
        float width=(float)(.78+.22*Math.sqrt(Math.max(0,progress)));
        return (x,y,z,u,v,layer)->sink.vertex(x*width,(float)(y*height(progress,u,layer,time)),z*width,u,v,layer);
    }
    private AuraReveal() {}
}
