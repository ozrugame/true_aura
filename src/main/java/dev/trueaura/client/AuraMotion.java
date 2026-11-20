package dev.trueaura.client;

/** Bounded, fixed-step spring in world axes. Rendering only interpolates its state. */
public final class AuraMotion {
    private final double[] value = new double[5], previous = new double[5], velocity = new double[4];
    private double lastX, lastY, lastZ, lastYaw;
    private boolean initialized;

    public void tick(double x, double y, double z, double yaw) {
        if (!Double.isFinite(x+y+z+yaw)) { reset(); return; }
        if (!initialized) { seed(x,y,z,yaw); return; }
        double dx=x-lastX, dy=y-lastY, dz=z-lastZ;
        double turn=((yaw-lastYaw)%360+540)%360-180;
        seed(x,y,z,yaw);
        if (dx*dx+dy*dy+dz*dz>16) { clearSpring(); return; }
        System.arraycopy(value,0,previous,0,value.length);
        spring(0,clamp(-dx*2.2,-.85,.85));
        spring(1,clamp(-dy*.7,-.4,.4));
        spring(2,clamp(-dz*2.2,-.85,.85));
        spring(3,clamp(-Math.toRadians(turn)*.65,-.65,.65));
        double energy=clamp(Math.sqrt(dx*dx+dz*dz)*2+Math.abs(dy)*.75+Math.abs(turn)*.008,0,1.5);
        value[4]+=(energy-value[4])*.18;
    }
    private void spring(int axis,double target) {
        velocity[axis]=clamp((velocity[axis]+(target-value[axis])*.22)*.72,-.4,.4);
        value[axis]=clamp(value[axis]+velocity[axis],-1,1);
    }
    private void seed(double x,double y,double z,double yaw) {
        initialized=true;lastX=x;lastY=y;lastZ=z;lastYaw=yaw;
    }
    public void reset() { initialized=false; clearSpring(); }
    private void clearSpring() { java.util.Arrays.fill(value,0);java.util.Arrays.fill(previous,0);java.util.Arrays.fill(velocity,0); }
    public Sample sample(float partial) {
        double t=Float.isFinite(partial)?clamp(partial,0,1):0;
        return new Sample(lerp(0,t),lerp(1,t),lerp(2,t),lerp(3,t),lerp(4,t));
    }
    private double lerp(int axis,double t) { return previous[axis]+(value[axis]-previous[axis])*t; }
    private static double clamp(double value,double min,double max) { return Math.max(min,Math.min(max,value)); }

    public record Sample(double x,double y,double z,double twist,double energy) {
        public AuraMesh.VertexSink wrap(AuraMesh.VertexSink sink,double height,double time,double strength) {
            if(strength==0) return sink;
            return (vx,vy,vz,u,v,layer) -> emit(vx,vy,vz,u,v,layer,height,time,strength,sink);
        }
        private void emit(float vx,float vy,float vz,float u,float v,float layer,double height,double time,double strength,AuraMesh.VertexSink sink) {
            double h=clamp(vy/Math.max(.1,height),0,1), weight=h*h;
            double angle=twist*weight*strength, sin=Math.sin(angle),cos=Math.cos(angle);
            double radius=Math.hypot(vx,vz);
            double ripple=Math.sin(h*10-time*9)*energy*.07*Math.sin(h*Math.PI)*strength;
            double factor=radius>.001?Math.max(0,1+ripple/radius):1;
            // Keep dy'/dy positive even for a 0.1-height imported surface: it may
            // compress/stretch, but cannot fold below its anchored base.
            double vertical=clamp(y*strength,-.35*Math.max(.1,height),.5*Math.max(.1,height));
            sink.vertex((float)((vx*cos-vz*sin)*factor+x*weight*strength),(float)(vy+vertical*weight),
                (float)((vx*sin+vz*cos)*factor+z*weight*strength),u,v,layer);
        }
        /** Convenience for geometry tests; production wraps the sink without per-vertex allocations. */
        public float[] deform(float vx,float vy,float vz,double height,double time,double strength) {
            if(strength==0) return new float[]{vx,vy,vz};
            float[] result=new float[3];
            emit(vx,vy,vz,0,0,0,height,time,strength,(a,b,c,u,v,l)->{result[0]=a;result[1]=b;result[2]=c;});
            return result;
        }
    }
}
