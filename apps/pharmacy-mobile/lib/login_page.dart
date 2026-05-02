import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import 'camera_upload_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final Dio _dio = Dio();

  final TextEditingController _apiBaseUrlController =
      TextEditingController(text: 'http://192.168.1.100:8082/api');
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  bool _loading = false;
  String _errorMessage = '';

  @override
  void dispose() {
    _apiBaseUrlController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() {
      _loading = true;
      _errorMessage = '';
    });

    try {
      final apiBaseUrl = _apiBaseUrlController.text.trim().replaceAll(RegExp(r'/+$'), '');
      final response = await _dio.post<Map<String, dynamic>>(
        '$apiBaseUrl/auth/login',
        data: {
          'email': _emailController.text.trim(),
          'password': _passwordController.text,
        },
      );

      final token = response.data?['accessToken'] as String?;
      if (token == null || token.isEmpty) {
        setState(() {
          _loading = false;
          _errorMessage = 'Login failed.';
        });
        return;
      }

      if (!mounted) {
        return;
      }

      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => CameraUploadPage(
            apiBaseUrl: apiBaseUrl,
            token: token,
          ),
        ),
      );
    } catch (e) {
      String message = 'Login failed.';
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
        _loading = false;
      });
      return;
    }

    setState(() {
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Pharmacy Login')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _apiBaseUrlController,
              decoration: const InputDecoration(
                labelText: 'API Base URL',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _emailController,
              decoration: const InputDecoration(
                labelText: 'Email',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _passwordController,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Password',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _loading ? null : _login,
              child: Text(_loading ? 'Logging in...' : 'Login'),
            ),
            if (_errorMessage.isNotEmpty) ...[
              const SizedBox(height: 10),
              Text(_errorMessage, style: const TextStyle(color: Colors.red)),
            ],
          ],
        ),
      ),
    );
  }
}
