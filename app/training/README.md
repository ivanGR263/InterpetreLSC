# Entrenamiento de gestos LSC (Administrador/a)

Este modulo permite que una persona administradora entrene el modelo de gestos y lo deje listo para la app Android.

## 1) Requisitos en computador

- Python 3.10+ instalado.
- Estar en la raiz del proyecto `LSCCONNECT`.

Instalar dependencias:

```bash
pip install -r app/training/requirements.txt
```

## 2) Preparar dataset

Usa un CSV con el formato:

- Columna 1: `label` (nombre del gesto, ejemplo `hola`)
- Columnas 2..64: `f0..f62` (21 landmarks * x,y,z = 63 valores)

Tienes plantilla base en:

- `app/training/dataset_template.csv`

Guarda tu dataset final, por ejemplo:

- `app/training/gestures_dataset.csv`

Recomendaciones:

- Minimo 30 muestras por gesto (ideal 100+).
- Balancear clases (cantidad parecida entre gestos).
- Normalizar el proceso de captura (distancia y luz similares).

## 3) Entrenar y exportar al app/assets

Ejecuta:

```bash
python app/training/train_gesture_classifier.py --data app/training/gestures_dataset.csv
```

El script genera automaticamente:

- `app/src/main/assets/gesture_classifier.tflite`
- `app/src/main/assets/gesture_labels.txt`

Opciones utiles:

```bash
python app/training/train_gesture_classifier.py \
  --data app/training/gestures_dataset.csv \
  --epochs 50 \
  --batch-size 32 \
  --val-ratio 0.2
```

## 4) Validar en Android Studio

1. Sincroniza Gradle.
2. Ejecuta la app en dispositivo.
3. Ve a la pantalla de camara.
4. Verifica que cambien:
   - Texto detectado
   - Confianza
   - Estado de deteccion

## 5) Flujo para ensenar nuevos gestos

1. Capturar nuevas muestras (CSV).
2. Mezclar con dataset anterior.
3. Reentrenar con el script.
4. Reemplazar automaticamente assets (lo hace el script).
5. Publicar nueva version de app.

---

Nota: este entrenamiento es offline en computador. La app Android usa el modelo ya exportado para inferencia en tiempo real.
