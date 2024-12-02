module.exports = {
  preset: "react-native",
  coverageProvider: "babel",
  ignorePatterns: [
    "*.test.{js,jsx,ts,tsx}",
    "*.spec.{js,jsx,ts,tsx}",
    "*.d.ts",
    "jest.config.js",
  ],
  coverageReporters: ["text", "lcov"],
  collectCoverageFrom: [
    "src/**/*.{js,jsx,ts,tsx}",
    "!src/**/*.d.ts",
    "!src/**/index.{js,ts}",
  ],
  coveragePathIgnorePatterns: [
    "/node_modules/",
    "/__tests__/",
    "/dist/",
    "\\.d\\.ts$",
    "/coverage/",
  ],
  testEnvironment: "node",
  reporters: [
    "default",
    [
      "jest-sonar-reporter",
      {
        outputDirectory: "coverage",
        outputName: "test-report.xml",
        sonarQubeVersion: "LATEST",
      },
    ],
  ],
  collectCoverage: true,
};
