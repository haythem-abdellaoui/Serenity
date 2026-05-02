import 'package:flutter/material.dart';

import 'login_page.dart';

void main() {
  runApp(const PharmacyMobileApp());
}

class PharmacyMobileApp extends StatelessWidget {
  const PharmacyMobileApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pharmacy Mobile',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1772C2)),
        useMaterial3: true,
      ),
      home: const LoginPage(),
    );
  }
}
