require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorWebauthnPlugin'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.ios.deployment_target  = '11.0'
  s.dependency 'Capacitor'
  s.dependency 'PromiseKit', '~> 6.13.1'
  s.dependency 'EllipticCurveKeyPair', '~> 2.0'
  s.dependency 'KeychainAccess', '~> 4.2.1'
  s.dependency 'CryptoSwift', '~> 1.3.8'
  s.swift_version = '5.1'
end
