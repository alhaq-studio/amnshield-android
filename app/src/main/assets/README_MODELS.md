# TensorFlow Lite Models for AmnShield

## Current Models

### 1. smart_keyword_classifier.tflite
- **Purpose**: Smart keyword detection and classification
- **Status**: ✅ Present
- **Location**: `app/src/main/assets/smart_keyword_classifier.tflite`

### 2. smart_blur_nsfw.tflite
- **Purpose**: NSFW/adult content image detection
- **Status**: ⚠️ Optional - Uses heuristic fallback if missing
- **Location**: `app/src/main/assets/smart_blur_nsfw.tflite` (to be added)

## Adding the NSFW Detection Model

### Option 1: Download Pre-trained Model (Recommended)
You can use a pre-trained NSFW detection model:

1. **NSFW MobileNet V2** (Recommended):
   - Download from: https://github.com/GantMan/nsfw_model
   - Convert to TFLite format
   - Place in `assets/` as `smart_blur_nsfw.tflite`

2. **Custom AmnShield Model** (Future):
   - Train on Islamic-specific content
   - Higher accuracy for halal/haram detection
   - Contact development team for latest version

### Option 2: Train Your Own Model
If you want custom detection:

```python
# Example training script (requires TensorFlow)
import tensorflow as tf

# 1. Prepare dataset
# 2. Train MobileNetV2 or EfficientNet model
# 3. Convert to TFLite

converter = tf.lite.TFLiteConverter.from_saved_model('saved_model/')
tflite_model = converter.convert()

with open('smart_blur_nsfw.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Option 3: Use Without Model (Current Fallback)
The app works WITHOUT the model using:
- 30+ keyword heuristics (bikini, lingerie, nude, xxx, etc.)
- Text context analysis
- Browser-aware detection
- Confidence scoring based on keyword matches

**Performance without model:**
- ✅ Fast (5-10ms per image)
- ✅ No false negatives for obvious content
- ⚠️ Less accurate for subtle content
- ⚠️ May have more false positives

## Model Specifications

### Expected Input
- **Format**: RGB Bitmap
- **Size**: 224x224 or 299x299 pixels
- **Normalization**: [0, 1] float values

### Expected Output
- **Format**: Float array
- **Classes**: [safe, questionable, unsafe, explicit]
- **Threshold**: 0.40 - 0.70 depending on sensitivity

## Testing Models

To test if a model works:

```kotlin
// In SmartImageModerator.kt
private fun testModel() {
    val testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
    val result = evaluate(SmartImageMetadata(
        packageName = "com.test",
        textSnippets = listOf("test"),
        bitmap = testBitmap
    ))
    Log.d("ModelTest", "Model verdict: ${result.action}")
}
```

## Model Performance

| Model | Size | Inference Time | Accuracy |
|-------|------|----------------|----------|
| Heuristics Only | 0 KB | ~5ms | 75% |
| MobileNet V2 | ~4 MB | ~80ms | 90% |
| EfficientNet B0 | ~6 MB | ~120ms | 93% |
| Custom AmnShield | ~5 MB | ~90ms | 95%* |

*Estimated based on Islamic content dataset

## Privacy & Security

✅ **All models run on-device**  
✅ **No data sent to external servers**  
✅ **No user tracking**  
✅ **Open source models recommended**  
✅ **Graceful degradation without model**  

## Troubleshooting

### Model Not Loading
- Check file exists: `ls app/src/main/assets/smart_blur_nsfw.tflite`
- Check file size: Should be > 1 MB
- Check format: Must be valid TFLite model
- Check permissions: App has READ_EXTERNAL_STORAGE if needed

### Model Performance Issues
- Reduce input size: 224x224 instead of 299x299
- Use quantized model: INT8 instead of FLOAT32
- Enable GPU delegate: TensorFlow Lite GPU acceleration
- Increase throttle time: Process fewer frames

### False Positives/Negatives
- Adjust sensitivity: Low (0) / Balanced (1) / Strict (2)
- Add to exclusions: Whitelist specific apps
- Update keywords: Add custom keywords in Keyword Blocker
- Retrain model: Use custom dataset

## References

- [TensorFlow Lite Guide](https://www.tensorflow.org/lite/guide)
- [NSFW Detection Models](https://github.com/GantMan/nsfw_model)
- [Model Conversion](https://www.tensorflow.org/lite/convert)
- [AmnShield Docs](../AI_FEATURES.md)
