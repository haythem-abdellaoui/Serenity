import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import 'login_page.dart';

class CameraUploadPage extends StatefulWidget {
  final String apiBaseUrl;
  final String token;

  const CameraUploadPage({
    super.key,
    required this.apiBaseUrl,
    required this.token,
  });

  @override
  State<CameraUploadPage> createState() => _CameraUploadPageState();
}

class _CameraUploadPageState extends State<CameraUploadPage> {
  final Dio _dio = Dio();
  final ImagePicker _picker = ImagePicker();
  final TextEditingController _prescriptionIdController = TextEditingController();

  XFile? _image;
  bool _loading = false;
  String _errorMessage = '';
  String _successMessage = '';

  @override
  void dispose() {
    _prescriptionIdController.dispose();
    super.dispose();
  }

  Future<void> _takePhoto() async {
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 85);
    if (image == null) {
      return;
    }

    setState(() {
      _image = image;
      _errorMessage = '';
      _successMessage = '';
    });
  }

  Future<void> _upload() async {
    final prescriptionId = int.tryParse(_prescriptionIdController.text.trim());
    if (prescriptionId == null || _image == null) {
      setState(() {
        _errorMessage = 'Enter prescription ID and take photo first.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final filePath = _image!.path;
      final fileName = filePath.split(Platform.pathSeparator).last;
      final data = FormData.fromMap({
        'file': await MultipartFile.fromFile(filePath, filename: fileName),
      });

      await _dio.post<Map<String, dynamic>>(
        '${widget.apiBaseUrl}/pharmacy/prescriptions/$prescriptionId/insurance-document',
        data: data,
        options: Options(
          headers: {
            'Authorization': 'Bearer ${widget.token}',
          },
        ),
      );

      setState(() {
        _successMessage = 'Upload success.';
        _image = null;
      });
    } catch (e) {
      String message = 'Upload failed.';
      if (e is DioException) {
        final body = e.response?.data;
        if (body is Map<String, dynamic> && body['message'] is String) {
          message = body['message'] as String;
        } else if (e.message != null) {
          message = e.message!;
        }
      }
      setState(() {
        _errorMessage = message;
      });
    }

    setState(() {
      _loading = false;
    });
  }

  void _logout() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Camera Upload'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _logout,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _prescriptionIdController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Prescription ID',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            FilledButton.icon(
              onPressed: _loading ? null : _takePhoto,
              icon: const Icon(Icons.photo_camera),
              label: const Text('Take Picture'),
            ),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed: _loading ? null : _upload,
              icon: const Icon(Icons.cloud_upload),
              label: Text(_loading ? 'Uploading...' : 'Upload'),
            ),
            if (_image != null) ...[
              const SizedBox(height: 10),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: Image.file(File(_image!.path), fit: BoxFit.cover),
                ),
              ),
            ] else
              const Spacer(),
            if (_errorMessage.isNotEmpty)
              Text(_errorMessage, style: const TextStyle(color: Colors.red)),
            if (_successMessage.isNotEmpty)
              Text(_successMessage, style: const TextStyle(color: Colors.green)),
          ],
        ),
      ),
    );
  }
}
