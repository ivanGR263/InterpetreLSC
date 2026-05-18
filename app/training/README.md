# Entrenamiento de gestos LSC (Administrador/a)

Este modulo permite que una persona administradora entrene el modelo de gestos y lo deje listo para la app Android.

## 1) Requisitos en computador

- Python 3.10+ instalado.
- Estar en la raiz del proyecto `NEXING`.

Instalar dependencias:

```bash
pip install -r app/training/requirements.txt
```

## 2) Preparar dataset

Usa un CSV con el formato:

- Columna 1: `label` (nombre del gesto, ejemplo `hola`)
- Columnas 2..64: `f0..f62` (21 landmarks * x,y,z = 63 valores)

Tienes plantillas base por cámara en:

- `app/training/dataset_template_front.csv` (cámara frontal)
- `app/training/dataset_template_back.csv` (cámara trasera)

Al capturar en modo administrador, la app guarda en el dispositivo:

- `gestures_dataset_front.csv`
- `gestures_dataset_back.csv`

Recomendaciones:

- Minimo 30 muestras por gesto (ideal 100+).
- Balancear clases (cantidad parecida entre gestos).
- Normalizar el proceso de captura (distancia y luz similares).

## 3) Entrenar y exportar al app/assets

Ejecuta:

```bash
python app/training/train_gesture_classifier.py --data app/training/dataset_template_front.csv
```

El script genera automaticamente:

- `app/src/main/assets/gesture_classifier.tflite`
- `app/src/main/assets/gesture_labels.txt`

Opciones utiles:

```bash
python app/training/train_gesture_classifier.py \
  --data app/training/dataset_template_front.csv \
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

# Planificación y Resultados Finales (Abril 2024)

## Modelo MoSCoW
Esta planificación refleja el estado del proyecto para la entrega final del 19 de abril.

| Prioridad | Tarea Técnica | Estado |
| :--- | :--- | :--- |
| **Must Have** | Detección de landmarks con MediaPipe y TFLite | Completado |
| **Must Have** | Traducción instantánea a Texto y Voz (TTS) | Completado |
| **Should Have** | Modo Administrador para captura de datasets | Completado |
| **Should Have** | Guías visuales y sugerencias ambientales | Completado |
| **Could Have** | Compartir datasets vía WhatsApp | Completado |
| **Won't Have** | Entrenamiento en el dispositivo (Edge Training) | Postergado |

## Resumen Ejecutivo para Presentación
- **Introducción:** NEXING es una app de asistencia social que traduce lengua de señas a texto y voz en tiempo real usando IA.
- **Objetivos:** Facilitar la comunicación inclusiva mediante visión artificial optimizada para móviles.
- **Resultados:** Detección estable con +85% de confianza y sistema de expansión de vocabulario funcional.
- **Conclusiones:** El uso de metodologías ágiles y el enfoque MoSCoW permitieron priorizar la estabilidad del núcleo de IA para la entrega final.

