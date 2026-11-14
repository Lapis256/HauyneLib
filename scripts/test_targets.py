import tempfile
import unittest
from pathlib import Path

from targets import load_targets


class TargetsTest(unittest.TestCase):
    def test_real_targets_keep_forge_out_of_release(self):
        targets = load_targets()
        self.assertEqual(targets['26.1']['minecraft'], '26.1.2')
        self.assertTrue(targets['26.1']['release'])
        self.assertFalse(targets['20.1']['release'])

    def test_rejects_invalid_declarations(self):
        for declaration in [
            'default=missing\n26.1=26.1.2,neoforge,true',
            'default=26.1\n26.1=26.1.2,unknown,true',
            'default=26.1\n26.1=26.1.2,neoforge,yes',
            'default=26.1\n26.1=26.1.2,neoforge,false',
        ]:
            with self.subTest(declaration=declaration), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / 'targets.properties'
                path.write_text(declaration)
                with self.assertRaises(ValueError):
                    load_targets(path)


if __name__ == '__main__':
    unittest.main()
