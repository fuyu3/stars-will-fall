import json, sys, numpy as np

def Rx(a):
    c,s=np.cos(a),np.sin(a); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(a):
    c,s=np.cos(a),np.sin(a); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def Rz(a):
    c,s=np.cos(a),np.sin(a); return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def euler(x,y,z):
    return Rz(np.radians(z)) @ Ry(np.radians(y)) @ Rx(np.radians(x))

# Tabela oficial do Minecraft (FaceBakery/FaceInfo): ordem exata dos 4 cantos de cada face
# e para qual canto de uv [u1,v1,u2,v2] cada vértice vai. Copiado 1:1 do jogo para o modelo
# ficar pixel-idêntico ao que o vanilla desenharia.
def corners(x0,y0,z0,x1,y1,z1):
    return {
        "down":  [(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)],
        "up":    [(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],
        "north": [(x1,y1,z0),(x1,y0,z0),(x0,y0,z0),(x0,y1,z0)],
        "south": [(x0,y1,z1),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1)],
        "west":  [(x0,y1,z0),(x0,y0,z0),(x0,y0,z1),(x0,y1,z1)],
        "east":  [(x1,y1,z1),(x1,y0,z1),(x1,y0,z0),(x1,y1,z0)],
    }
UV_ORDER = [(0,1),(0,3),(2,3),(2,1)]  # idx0->(u1,v1) idx1->(u1,v2) idx2->(u2,v2) idx3->(u2,v1)

def convert(model_json, obj_path, mtl_name, mat_name, texture_resloc):
    elements = model_json["elements"]
    verts=[]; norms=[]; uvs=[]; faces=[]  # faces: list of (v_idx1based x4, n_idx, uv_idx1based x4)

    for el in elements:
        x0,y0,z0 = el["from"]; x1,y1,z1 = el["to"]
        rot = el.get("rotation")
        if rot and "axis" in rot:
            angle = rot.get("angle",0.0); axis = rot["axis"]
            x=angle if axis=="x" else 0.0
            y=angle if axis=="y" else 0.0
            z=angle if axis=="z" else 0.0
            origin = rot["origin"]
        elif rot:
            x=rot.get("x",0.0); y=rot.get("y",0.0); z=rot.get("z",0.0); origin=rot["origin"]
        else:
            x=y=z=0.0; origin=[0,0,0]
        R = euler(x,y,z)
        origin = np.array(origin, dtype=float)

        cs = corners(x0,y0,z0,x1,y1,z1)
        for face, pts in cs.items():
            if face not in el["faces"]:
                continue
            fd = el["faces"][face]
            u1,v1,u2,v2 = fd["uv"]

            world_pts=[]
            for p in pts:
                p = np.array(p, dtype=float)
                p = R @ (p - origin) + origin
                world_pts.append(p)
            v1v = np.array(world_pts[1]) - np.array(world_pts[0])
            v2v = np.array(world_pts[2]) - np.array(world_pts[1])
            normal = np.cross(v1v, v2v)
            n = normal / (np.linalg.norm(normal) + 1e-9)
            n = R @ np.array([0.0,0.0,0.0]) * 0 + n  # normal já está em espaço mundo (rotacionada via pontos)

            vidx=[]
            for p in world_pts:
                verts.append(p / 16.0)  # Minecraft usa 0-16 por bloco; OBJ = 1 unidade por bloco
                vidx.append(len(verts))
            norms.append(n); nidx=len(norms)
            uvidx=[]
            for (ui,vi) in UV_ORDER:
                uvs.append(((u1,u2)[ui==2]/16.0, (v1,v2)[vi==3]/16.0))
                uvidx.append(len(uvs))
            faces.append((vidx,nidx,uvidx))

    with open(obj_path,"w") as f:
        f.write(f"mtllib {mtl_name}\n")
        for p in verts: f.write(f"v {p[0]:.6f} {p[1]:.6f} {p[2]:.6f}\n")
        for n in norms: f.write(f"vn {n[0]:.6f} {n[1]:.6f} {n[2]:.6f}\n")
        for uv in uvs: f.write(f"vt {uv[0]:.6f} {uv[1]:.6f}\n")
        f.write(f"usemtl {mat_name}\n")
        for vidx,nidx,uvidx in faces:
            a=f"{vidx[0]}/{uvidx[0]}/{nidx}"
            b=f"{vidx[1]}/{uvidx[1]}/{nidx}"
            c=f"{vidx[2]}/{uvidx[2]}/{nidx}"
            d=f"{vidx[3]}/{uvidx[3]}/{nidx}"
            f.write(f"f {a} {b} {c} {d}\n")
    print(f"{obj_path}: {len(verts)} vértices, {len(faces)} faces")

if __name__ == "__main__":
    import argparse
    ap = argparse.ArgumentParser(description=(
        "Converte um item model exportado do Blockbench (Java Block/Item, com 'elements') "
        "para .obj, porque esse formato do Minecraft só aceita rotacao em UM eixo e em "
        "incrementos de 22.5 graus (-45 a 45) por elemento -- qualquer coisa alem disso "
        "(varios eixos ao mesmo tempo, ou angulos como -35/25/90) faz o Minecraft REJEITAR "
        "o modelo inteiro e mostrar o bloco preto e rosa de 'model ausente'. O .obj nao tem "
        "essa restricao, entao aqui a peca fica com a MESMA geometria e a MESMA rotacao que "
        "aparece no Blockbench."
    ))
    ap.add_argument("model_json", help="o .json exportado do Blockbench (Java Block/Item)")
    ap.add_argument("obj_out", help="caminho do .obj de saida")
    ap.add_argument("--mtl", default=None, help="nome do arquivo .mtl (mtllib), padrao: <obj_out sem extensao>.mtl")
    ap.add_argument("--material", default="material", help="nome do material dentro do .obj/.mtl")
    args = ap.parse_args()

    import json, pathlib
    model = json.load(open(args.model_json))
    mtl_name = args.mtl or (pathlib.Path(args.obj_out).stem + ".mtl")
    convert(model, args.obj_out, mtl_name, args.material, texture_resloc="(preencha o .mtl à mão)")
    print(f"Pronto. Crie {mtl_name} do lado do .obj com:\n"
          f"  newmtl {args.material}\n  map_Kd <namespace>:item/<nome_da_textura>")
