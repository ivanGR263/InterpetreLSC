#!/usr/bin/env python3
"""
Entrena un clasificador de gestos LSC usando landmarks (63 features)
y exporta:
  - gesture_classifier.tflite
  - gesture_labels.txt

Formato esperado del CSV:
  label,f0,f1,...,f62
  hola,0.12,0.34,...,-0.09
"""

from __future__ import annotations

import argparse
import csv
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

import numpy as np
import tensorflow as tf


FEATURE_COUNT = 63  # 21 landmarks * (x,y,z)
RNG_SEED = 42


@dataclass
class Dataset:
    x_train: np.ndarray
    y_train: np.ndarray
    x_val: np.ndarray
    y_val: np.ndarray
    labels: List[str]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Entrena y exporta modelo TFLite para NEXING."
    )
    parser.add_argument(
        "--data",
        required=True,
        help="Ruta CSV con columnas: label,f0...f62",
    )
    parser.add_argument(
        "--epochs",
        type=int,
        default=35,
        help="Numero de epocas (default: 35).",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=32,
        help="Batch size (default: 32).",
    )
    parser.add_argument(
        "--val-ratio",
        type=float,
        default=0.2,
        help="Proporcion para validacion por clase (default: 0.2).",
    )
    parser.add_argument(
        "--model-out",
        default="app/src/main/assets/gesture_classifier.tflite",
        help="Ruta de salida del modelo TFLite.",
    )
    parser.add_argument(
        "--labels-out",
        default="app/src/main/assets/gesture_labels.txt",
        help="Ruta de salida de etiquetas.",
    )
    return parser.parse_args()


def load_rows(csv_path: Path) -> List[Tuple[str, np.ndarray]]:
    rows: List[Tuple[str, np.ndarray]] = []
    with csv_path.open("r", newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader, None)
        if header is None:
            raise ValueError("CSV vacio.")

        if len(header) < FEATURE_COUNT + 1:
            raise ValueError(
                f"Header invalido. Se esperaban {FEATURE_COUNT + 1} columnas."
            )

        for i, row in enumerate(reader, start=2):
            if not row:
                continue
            if len(row) < FEATURE_COUNT + 1:
                raise ValueError(
                    f"Fila {i} invalida: se esperaban {FEATURE_COUNT + 1} columnas."
                )
            label = row[0].strip()
            if not label:
                raise ValueError(f"Fila {i} invalida: etiqueta vacia.")

            try:
                features = np.asarray(row[1 : FEATURE_COUNT + 1], dtype=np.float32)
            except ValueError as exc:
                raise ValueError(f"Fila {i} invalida: features no numericos.") from exc

            rows.append((label, features))

    if not rows:
        raise ValueError("No hay muestras en el CSV.")
    return rows


def stratified_split(
    rows: Sequence[Tuple[str, np.ndarray]], val_ratio: float
) -> Tuple[List[Tuple[str, np.ndarray]], List[Tuple[str, np.ndarray]]]:
    random.seed(RNG_SEED)
    by_label: Dict[str, List[Tuple[str, np.ndarray]]] = {}
    for item in rows:
        by_label.setdefault(item[0], []).append(item)

    train_items: List[Tuple[str, np.ndarray]] = []
    val_items: List[Tuple[str, np.ndarray]] = []

    for label, items in by_label.items():
        random.shuffle(items)
        if len(items) < 5:
            raise ValueError(
                f"La clase '{label}' tiene {len(items)} muestras. "
                "Se recomiendan al menos 5 por clase."
            )

        val_count = max(1, int(len(items) * val_ratio))
        val_items.extend(items[:val_count])
        train_items.extend(items[val_count:])

    random.shuffle(train_items)
    random.shuffle(val_items)
    return train_items, val_items


def to_arrays(
    train_items: Sequence[Tuple[str, np.ndarray]],
    val_items: Sequence[Tuple[str, np.ndarray]],
) -> Dataset:
    labels = sorted({label for label, _ in train_items + val_items})
    label_to_idx = {label: i for i, label in enumerate(labels)}

    x_train = np.stack([features for _, features in train_items]).astype(np.float32)
    y_train = np.asarray([label_to_idx[label] for label, _ in train_items], dtype=np.int32)
    x_val = np.stack([features for _, features in val_items]).astype(np.float32)
    y_val = np.asarray([label_to_idx[label] for label, _ in val_items], dtype=np.int32)

    return Dataset(
        x_train=x_train,
        y_train=y_train,
        x_val=x_val,
        y_val=y_val,
        labels=labels,
    )


def build_model(num_classes: int) -> tf.keras.Model:
    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(FEATURE_COUNT,)),
            tf.keras.layers.Dense(128, activation="relu"),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(64, activation="relu"),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(num_classes, activation="softmax"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    return model


def save_labels(labels: Sequence[str], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(labels) + "\n", encoding="utf-8")


def export_tflite(model: tf.keras.Model, output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    output_path.write_bytes(tflite_model)


def main() -> None:
    args = parse_args()
    csv_path = Path(args.data)
    model_out = Path(args.model_out)
    labels_out = Path(args.labels_out)

    if not csv_path.exists():
        raise FileNotFoundError(f"No existe el archivo de datos: {csv_path}")

    np.random.seed(RNG_SEED)
    tf.random.set_seed(RNG_SEED)

    rows = load_rows(csv_path)
    train_items, val_items = stratified_split(rows, val_ratio=args.val_ratio)
    dataset = to_arrays(train_items, val_items)

    print(f"Clases: {dataset.labels}")
    print(
        f"Muestras train={len(dataset.x_train)}, val={len(dataset.x_val)}, "
        f"features={dataset.x_train.shape[1]}"
    )

    model = build_model(num_classes=len(dataset.labels))
    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_accuracy", patience=7, restore_best_weights=True
        )
    ]
    history = model.fit(
        dataset.x_train,
        dataset.y_train,
        validation_data=(dataset.x_val, dataset.y_val),
        epochs=args.epochs,
        batch_size=args.batch_size,
        verbose=1,
        callbacks=callbacks,
    )

    loss, acc = model.evaluate(dataset.x_val, dataset.y_val, verbose=0)
    print(f"Validacion -> loss={loss:.4f} acc={acc:.4f}")
    print(f"Epocas entrenadas: {len(history.history['loss'])}")

    export_tflite(model, model_out)
    save_labels(dataset.labels, labels_out)

    print(f"Modelo exportado en: {model_out}")
    print(f"Etiquetas exportadas en: {labels_out}")
    print("Listo para usar en NEXING.")


if __name__ == "__main__":
    main()
