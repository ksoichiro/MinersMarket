#!/usr/bin/env python3
"""
NBT Structure Converter: 1.21.1 → 1.20.1

Converts Minecraft structure NBT files from 1.21.1 format (data components)
to 1.20.1 format (NBT tags) for multi-version support.

Key conversions:
- Item format: components → tag
- Field names: count (int) → Count (byte), slot (int) → Slot (byte)
- Container items in block_entities

Usage:
    python3 convert_nbt_1_21_to_1_20.py <input.nbt> <output.nbt>
    python3 convert_nbt_1_21_to_1_20.py --batch <input_dir> <output_dir>
    python3 convert_nbt_1_21_to_1_20.py --batch --data-version 3120 <input_dir> <output_dir>
"""

import sys
import argparse
import gzip
import io
from pathlib import Path
from typing import Any

try:
    import nbtlib
    from nbtlib import tag
except ImportError:
    print("Error: nbtlib not installed. Install with: pip3 install nbtlib", file=sys.stderr)
    sys.exit(1)


def convert_components_to_tag(components: Any) -> tag.Compound:
    """
    Convert 1.21.1 item components to 1.20.1 NBT tag format.

    Args:
        components: Dictionary of item components from 1.21.1

    Returns:
        nbtlib.tag.Compound representing NBT tag format for 1.20.1
    """
    tag_data = {}

    # minecraft:custom_data → direct copy to tag
    if "minecraft:custom_data" in components:
        custom_data = components["minecraft:custom_data"]
        if isinstance(custom_data, (dict, tag.Compound)):
            tag_data.update(dict(custom_data))

    # minecraft:enchantments → tag.Enchantments
    if "minecraft:enchantments" in components:
        enchant_comp = components["minecraft:enchantments"]
        if isinstance(enchant_comp, (dict, tag.Compound)) and "levels" in enchant_comp:
            enchantments = []
            levels = enchant_comp["levels"]
            if isinstance(levels, (dict, tag.Compound)):
                for ench_id, level in levels.items():
                    enchantments.append(tag.Compound({
                        "id": tag.String(str(ench_id)),
                        "lvl": tag.Short(int(level))
                    }))
            tag_data["Enchantments"] = tag.List[tag.Compound](enchantments)

    # minecraft:display → tag.display
    display_data = {}
    if "minecraft:custom_name" in components:
        display_data["Name"] = tag.String(str(components["minecraft:custom_name"]))
    if "minecraft:lore" in components:
        display_data["Lore"] = components["minecraft:lore"]
    if display_data:
        tag_data["display"] = tag.Compound(display_data)

    # minecraft:damage → tag.Damage
    if "minecraft:damage" in components:
        tag_data["Damage"] = tag.Int(int(components["minecraft:damage"]))

    # minecraft:unbreakable → tag.Unbreakable
    if "minecraft:unbreakable" in components:
        unbreakable = components["minecraft:unbreakable"]
        if isinstance(unbreakable, (dict, tag.Compound)) and unbreakable.get("show_in_tooltip", True):
            tag_data["Unbreakable"] = tag.Byte(1)

    return tag.Compound(tag_data) if tag_data else tag.Compound({})


def convert_item_1_21_to_1_20(item_1_21: Any) -> tag.Compound:
    """
    Convert a single item from 1.21.1 format to 1.20.1 format.

    1.21.1 format:
        {slot: 0, item: {id: "minecraft:stone", count: 1, components: {...}}}

    1.20.1 format:
        {Slot: 0b, id: "minecraft:stone", Count: 1b, tag: {...}}

    Args:
        item_1_21: Item compound in 1.21.1 format

    Returns:
        Item compound in 1.20.1 format
    """
    item_1_20 = {}

    # slot (int) → Slot (byte)
    if "slot" in item_1_21:
        slot_val = item_1_21["slot"]
        item_1_20["Slot"] = tag.Byte(int(slot_val))

    # Extract item data from nested "item" field
    item_data = item_1_21.get("item", {})

    # id → id (string)
    if "id" in item_data:
        item_1_20["id"] = tag.String(str(item_data["id"]))

    # count (int) → Count (byte)
    if "count" in item_data:
        count_val = item_data["count"]
        item_1_20["Count"] = tag.Byte(int(count_val))

    # components → tag
    if "components" in item_data:
        components = item_data["components"]
        if isinstance(components, (dict, tag.Compound)) and components:
            tag_data = convert_components_to_tag(components)
            if tag_data and len(tag_data) > 0:
                item_1_20["tag"] = tag_data

    return tag.Compound(item_1_20)


def convert_legacy_item_format(item: Any) -> tag.Compound:
    """
    Convert legacy/mixed format items to proper 1.20.1 format.

    Handles items with format: {Slot, id, count} (lowercase count)
    Converts to: {Slot, id, Count} (uppercase Count, Byte type)

    Args:
        item: Item compound in legacy/mixed format

    Returns:
        Item compound in 1.20.1 format
    """
    item_1_20 = {}

    # Copy Slot (already correct if present)
    if 'Slot' in item:
        slot_val = item['Slot']
        if isinstance(slot_val, tag.Byte):
            item_1_20['Slot'] = slot_val
        else:
            item_1_20['Slot'] = tag.Byte(int(slot_val))

    # Copy id (already correct if present)
    if 'id' in item:
        item_1_20['id'] = tag.String(str(item['id']))

    # Convert count → Count
    if 'count' in item:
        count_val = item['count']
        item_1_20['Count'] = tag.Byte(int(count_val))
    elif 'Count' in item:
        count_val = item['Count']
        if isinstance(count_val, tag.Byte):
            item_1_20['Count'] = count_val
        else:
            item_1_20['Count'] = tag.Byte(int(count_val))

    # Copy tag if present (already in 1.20.1 format)
    if 'tag' in item:
        item_1_20['tag'] = item['tag']

    return tag.Compound(item_1_20)


def convert_block_entity(block_entity: Any) -> tag.Compound:
    """
    Convert a block entity from 1.21.1 to 1.20.1 format.

    Handles container block entities (chests, barrels, etc.) by converting
    their Items field, and item frames by converting their Item field.

    Args:
        block_entity: Block entity compound in 1.21.1 format

    Returns:
        Block entity compound in 1.20.1 format
    """
    converted = dict(block_entity)

    if "Items" in block_entity:
        items = block_entity["Items"]
        if isinstance(items, (list, nbtlib.tag.List)):
            converted_items = []
            for item in items:
                if isinstance(item, (dict, tag.Compound)):
                    if 'item' in item:
                        # New 1.21.1 format: {slot, item: {id, count, components}}
                        converted_item = convert_item_1_21_to_1_20(item)
                    else:
                        # Legacy/mixed format: {Slot, id, count}
                        converted_item = convert_legacy_item_format(item)
                    converted_items.append(converted_item)
                else:
                    converted_items.append(item)
            converted["Items"] = tag.List[tag.Compound](converted_items)

    # Item frames use singular "Item" field: {id, count, components}
    # Convert to 1.20.1 format: {id, Count, tag}
    if "Item" in block_entity:
        item = block_entity["Item"]
        if isinstance(item, (dict, tag.Compound)):
            item_1_20 = {}
            if "id" in item:
                item_1_20["id"] = tag.String(str(item["id"]))
            if "count" in item:
                item_1_20["Count"] = tag.Byte(int(item["count"]))
            elif "Count" in item:
                count_val = item["Count"]
                item_1_20["Count"] = tag.Byte(int(count_val)) if not isinstance(count_val, tag.Byte) else count_val
            if "components" in item:
                components = item["components"]
                if isinstance(components, (dict, tag.Compound)) and components:
                    tag_data = convert_components_to_tag(components)
                    if tag_data and len(tag_data) > 0:
                        item_1_20["tag"] = tag_data
            converted["Item"] = tag.Compound(item_1_20)

    return tag.Compound(converted)


def save_nbt_deterministic(nbt_data: nbtlib.File, output_path: str) -> None:
    """Save NBT file with deterministic output (fixed mtime for gzip)."""
    buffer = io.BytesIO()
    nbt_data.write(buffer, nbt_data.byteorder)
    uncompressed_data = buffer.getvalue()
    compressed_data = gzip.compress(uncompressed_data, compresslevel=9, mtime=0)
    with open(output_path, 'wb') as f:
        f.write(compressed_data)


def convert_nbt_structure(input_path: str, output_path: str, data_version: int = 3465) -> bool:
    """
    Convert an NBT structure file from 1.21.1 to 1.20.1 format.

    Handles:
    - Separate block_entities field
    - Inline NBT data in blocks[].nbt
    - Entity items (e.g. item frames in entities[].nbt.Item)

    Args:
        input_path: Path to input NBT file (1.21.1 format)
        output_path: Path to output NBT file (1.20.1 format)

    Returns:
        True if conversion successful, False otherwise
    """
    try:
        nbt_data = nbtlib.load(input_path)

        if "DataVersion" in nbt_data:
            original_version = nbt_data["DataVersion"]
            nbt_data["DataVersion"] = tag.Int(data_version)
            print(f"  Updated DataVersion: {original_version} -> {data_version}")

        converted_count = 0

        # Method 1: Convert separate block_entities field (if exists)
        if "block_entities" in nbt_data:
            block_entities = nbt_data["block_entities"]
            if isinstance(block_entities, (list, nbtlib.tag.List)) and len(block_entities) > 0:
                converted_block_entities = []
                for block_entity in block_entities:
                    if isinstance(block_entity, (dict, tag.Compound)):
                        converted_be = convert_block_entity(block_entity)
                        converted_block_entities.append(converted_be)
                        converted_count += 1
                    else:
                        converted_block_entities.append(block_entity)

                nbt_data["block_entities"] = tag.List[tag.Compound](converted_block_entities)

        # Method 2: Convert inline NBT data in blocks[].nbt
        if "blocks" in nbt_data:
            blocks = nbt_data["blocks"]
            if isinstance(blocks, (list, nbtlib.tag.List)):
                for block in blocks:
                    if isinstance(block, (dict, tag.Compound)) and 'nbt' in block:
                        block_nbt = block['nbt']
                        if isinstance(block_nbt, (dict, tag.Compound)) and 'Items' in block_nbt:
                            converted_nbt = convert_block_entity(block_nbt)
                            block['nbt'] = converted_nbt
                            converted_count += 1

        # Method 3: Convert entity items (e.g. item frames in entities[].nbt.Item)
        entity_count = 0
        if "entities" in nbt_data:
            entities = nbt_data["entities"]
            if isinstance(entities, (list, nbtlib.tag.List)):
                for entity in entities:
                    if not isinstance(entity, (dict, tag.Compound)):
                        continue
                    entity_nbt = entity.get("nbt", None)
                    if not isinstance(entity_nbt, (dict, tag.Compound)):
                        continue
                    if "Item" in entity_nbt:
                        item = entity_nbt["Item"]
                        if isinstance(item, (dict, tag.Compound)):
                            item_1_20 = {}
                            if "id" in item:
                                item_1_20["id"] = tag.String(str(item["id"]))
                            if "count" in item:
                                item_1_20["Count"] = tag.Byte(int(item["count"]))
                            elif "Count" in item:
                                count_val = item["Count"]
                                item_1_20["Count"] = tag.Byte(int(count_val)) if not isinstance(count_val, tag.Byte) else count_val
                            if "components" in item:
                                components = item["components"]
                                if isinstance(components, (dict, tag.Compound)) and components:
                                    tag_data = convert_components_to_tag(components)
                                    if tag_data and len(tag_data) > 0:
                                        item_1_20["tag"] = tag_data
                            entity_nbt["Item"] = tag.Compound(item_1_20)
                            entity_count += 1

        save_nbt_deterministic(nbt_data, output_path)

        if converted_count > 0 or entity_count > 0:
            print(f"  Converted {converted_count} container(s), {entity_count} entity item(s)")
        else:
            print(f"  No containers found, saved with updated DataVersion")

        return True

    except Exception as e:
        print(f"  Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return False


def convert_batch(input_dir: str, output_dir: str, data_version: int = 3465) -> int:
    """Convert all NBT files in a directory."""
    input_path = Path(input_dir)
    output_path = Path(output_dir)

    if not input_path.exists():
        print(f"Error: Input directory does not exist: {input_dir}", file=sys.stderr)
        return 0

    output_path.mkdir(parents=True, exist_ok=True)

    nbt_files = list(input_path.glob("*.nbt"))
    if not nbt_files:
        print(f"Warning: No .nbt files found in {input_dir}")
        return 0

    print(f"Converting {len(nbt_files)} NBT files...")
    print(f"  Input:  {input_dir}")
    print(f"  Output: {output_dir}")
    print()

    success_count = 0
    for nbt_file in nbt_files:
        output_file = output_path / nbt_file.name
        print(f"Converting: {nbt_file.name}")
        if convert_nbt_structure(str(nbt_file), str(output_file), data_version):
            success_count += 1

    print()
    print(f"Conversion complete: {success_count}/{len(nbt_files)} files converted successfully")
    return success_count


def main():
    parser = argparse.ArgumentParser(
        description="Convert Minecraft structure NBT files from 1.21.1 to older format"
    )
    parser.add_argument(
        "--batch", action="store_true",
        help="Batch convert all NBT files in input directory"
    )
    parser.add_argument(
        "--data-version", type=int, default=3465,
        help="Target DataVersion (default: 3465 for 1.20.1, use 3120 for 1.19.2)"
    )
    parser.add_argument("input", help="Input NBT file (or directory if --batch)")
    parser.add_argument("output", help="Output NBT file (or directory if --batch)")

    args = parser.parse_args()

    if args.batch:
        success_count = convert_batch(args.input, args.output, args.data_version)
        sys.exit(0 if success_count > 0 else 1)
    else:
        print(f"Converting: {args.input} -> {args.output}")
        success = convert_nbt_structure(args.input, args.output, args.data_version)
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
